package com.banuba.sample.videotransformers;

import android.graphics.Bitmap;

import com.opentok.android.BaseVideoRenderer.Frame;
import com.opentok.android.PublisherKit.CustomVideoTransformer;

import java.nio.ByteBuffer;

public class BanubaTransformer implements CustomVideoTransformer {

    public BanubaTransformer(Bitmap image) {
        this.image = image;
    }

    @Override
    public void onTransform(Frame frame) {

        // Obtain the Y  plane of the video frame
        ByteBuffer yPlane = frame.getYplane();

        // Get the dimensions of the video frame
        int videoWidth = frame.getWidth();
        int videoHeight = frame.getHeight();

        // Calculate the desired size of the image
        int desiredWidth = videoWidth / 8; // Adjust this value as needed
        int desiredHeight = (int) (image.getHeight() * ((float) desiredWidth / image.getWidth()));

        // Resize the image to the desired size
        image = resizeImage(image, desiredWidth, desiredHeight);

        int logoWidth = image.getWidth();
        int logoHeight = image.getHeight();

        // Location of the image (center of video)
        int logoPositionX = videoWidth * 1 / 2 - logoWidth; // Adjust this as needed for the desired position
        int logoPositionY = videoHeight * 1 / 2 - logoHeight; // Adjust this as needed for the desired position

        // Overlay the logo on the video frame
        for (int y = 0; y < logoHeight; y++) {
            for (int x = 0; x < logoWidth; x++) {
                int frameOffset = (logoPositionY + y) * videoWidth + (logoPositionX + x);

                // Get the logo pixel color
                int logoPixel = image.getPixel(x, y);

                // Extract the color channels (ARGB)
                int logoAlpha = (logoPixel >> 24) & 0xFF;
                int logoRed = (logoPixel >> 16) & 0xFF;

                // Overlay the logo pixel on the video frame
                int framePixel = yPlane.get(frameOffset) & 0xFF;

                // Calculate the blended pixel value
                int blendedPixel = ((logoAlpha * logoRed + (255 - logoAlpha) * framePixel) / 255) & 0xFF;

                // Set the blended pixel value in the video frame
                yPlane.put(frameOffset, (byte) blendedPixel);
            }
        }
    }
    private Bitmap resizeImage(Bitmap image, int width, int height) {
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
    private Bitmap image;
}
