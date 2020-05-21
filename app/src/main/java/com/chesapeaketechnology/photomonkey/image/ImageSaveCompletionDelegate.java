package com.chesapeaketechnology.photomonkey.image;

import java.io.File;

public interface ImageSaveCompletionDelegate {
    public void imageWasSaved(File photoFile);
}
