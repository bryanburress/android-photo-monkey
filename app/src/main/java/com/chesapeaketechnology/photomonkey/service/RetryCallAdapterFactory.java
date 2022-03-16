package com.chesapeaketechnology.photomonkey.service;


import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import timber.log.Timber;

/**
 * Assists with retrying API calls to the Rest API. If the API call is annotated with
 * the {@link Retry} annotation, then the REST call will be retried the specified number of times. If no annotation is
 * present, then the REST call is tried only one time.
 *
 * @since 0.2.0
 */
public class RetryCallAdapterFactory extends CallAdapter.Factory
{
    private Handler restHandler;

    public static RetryCallAdapterFactory create()
    {
        final RetryCallAdapterFactory instance = new RetryCallAdapterFactory();
        instance.initHandlerThread();
        return instance;
    }

    /**
     * Initializes the Handler thread by constructing it, starting the thread, and then using the thread's looper to
     * create a handler object that can be used for retrying REST calls. We don't do this in the constructor of this
     * class because it is unsafe to start a thread in a constructor.
     */
    private void initHandlerThread()
    {
        HandlerThread restHandlerThread = new HandlerThread("PhotoMonkeyRetryRestThread");
        restHandlerThread.start();
        restHandler = new Handler(restHandlerThread.getLooper());
    }

    @Nullable
    @Override
    public CallAdapter<?, ?> get(@NonNull Type returnType, @NonNull Annotation[] annotations, @NonNull Retrofit retrofit)
    {
        /**
         * You can setup a default max retry count for all connections.
         */
        int itShouldRetry = 0;
        final Retry retry = getRetry(annotations);
        if (retry != null)
        {
            itShouldRetry = retry.max();
        }
        Timber.d("Starting a CallAdapter with %s retries.", itShouldRetry);
        return new RetryCallAdapter<>(
                retrofit.nextCallAdapter(this, returnType, annotations),
                itShouldRetry,
                restHandler
        );
    }

    private Retry getRetry(@NonNull Annotation[] annotations)
    {
        for (Annotation annotation : annotations)
        {
            if (annotation instanceof Retry)
            {
                return (Retry) annotation;
            }
        }
        return null;
    }

    static final class RetryCallAdapter<R, T> implements CallAdapter<R, T>
    {
        private final CallAdapter<R, T> delegated;
        private final int maxRetries;
        private final Handler restHandler;

        public RetryCallAdapter(CallAdapter<R, T> delegated, int maxRetries, Handler restHandler)
        {
            this.delegated = delegated;
            this.maxRetries = maxRetries;
            this.restHandler = restHandler;
        }

        @Override
        public Type responseType()
        {
            return delegated.responseType();
        }

        @Override
        public T adapt(final @NotNull Call<R> call)
        {
            return delegated.adapt(maxRetries > 0 ? new RetryingCall<>(call, maxRetries, restHandler) : call);
        }
    }

    static final class RetryingCall<R> implements Call<R>
    {
        private final Call<R> delegated;
        private final int maxRetries;
        private final Handler restHandler;

        public RetryingCall(Call<R> delegated, int maxRetries, Handler restHandler)
        {
            this.delegated = delegated;
            this.maxRetries = maxRetries;
            this.restHandler = restHandler;
        }

        @Override
        public Response<R> execute() throws IOException
        {
            return delegated.execute();
        }

        @Override
        public void enqueue(@NonNull Callback<R> callback)
        {
            delegated.enqueue(new RetryCallback<>(delegated, callback, maxRetries, restHandler));
        }

        @Override
        public boolean isExecuted()
        {
            return delegated.isExecuted();
        }

        @Override
        public void cancel()
        {
            delegated.cancel();
        }

        @Override
        public boolean isCanceled()
        {
            return delegated.isCanceled();
        }

        @Override
        public Call<R> clone()
        {
            return new RetryingCall<>(delegated.clone(), maxRetries, restHandler);
        }

        @Override
        public Request request()
        {
            return delegated.request();
        }

        @NonNull
        @Override
        public Timeout timeout()
        {
            return new Timeout().timeout(2L, TimeUnit.SECONDS);
        }
    }

    static final class RetryCallback<T> implements Callback<T>
    {
        private final Call<T> call;
        private final Callback<T> callback;
        private final int maxRetries;
        private final Handler restHandler;
        private final AtomicInteger retryCount = new AtomicInteger(0);

        public RetryCallback(Call<T> call, Callback<T> callback, int maxRetries, Handler restHandler)
        {
            this.call = call;
            this.callback = callback;
            this.maxRetries = maxRetries;
            this.restHandler = restHandler;
        }

        @Override
        public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response)
        {
            if (!response.isSuccessful() && retryCount.incrementAndGet() <= maxRetries)
            {
                Timber.d("Call with no success result code: %s ", response.code());
                retryCall();
            } else
            {
                callback.onResponse(call, response);
            }
        }

        @Override
        public void onFailure(@NonNull Call<T> call, @NonNull Throwable t)
        {
            Timber.d(t, "REST API Call failed");
            if (retryCount.incrementAndGet() <= maxRetries)
            {
                retryCall();
            } else if (maxRetries > 0)
            {
                Timber.d("No retries left sending timeout up.");
                callback.onFailure(call,
                        new TimeoutException(String.format("No retries left after %s attempts.", maxRetries)));
            } else
            {
                callback.onFailure(call, t);
            }
        }

        /**
         * Handles scheduling the retry of the REST API call. This method calculates a delay to use for the retry and
         * then schedules the call based on that delay.
         */
        private void retryCall()
        {
            final int currentRetryAttempt = retryCount.get();
            // Introduces a delay in the retry to give the server time to come back online or the network issues to
            // resolve. There is also a random number added for jitter to add some variance for all the clients.
            // First retry would be 1 * 1 * 2 + jitter ~= 2 seconds
            // ... Fifth retry would be 5 * 5 * 2 + jitter ~= 50 seconds
            // Leaving this exponential backoff retry in case we want to make it configurable in the @Retry annotation
            //int retryDelayMs = (currentRetryAttempt * currentRetryAttempt * 2) * 1000 + new Random().nextInt(3000);

            // Linear delay instead of an exponential delay.
            int retryDelayMs = currentRetryAttempt * 1000;

            Timber.w("Retrying API call. count=%d, maxRetries=%d, retryDelayMs=%d", currentRetryAttempt, maxRetries, retryDelayMs);

            restHandler.postDelayed(() -> call.clone().enqueue(this), retryDelayMs);
        }
    }
}
