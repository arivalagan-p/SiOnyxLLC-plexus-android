package com.dome.librarynightwave.model.repository.mediaCodec;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class H264Utils {
    private final String TAG = "H264Utils";

    private static H264Utils h264Utils;
    private byte[] spsData;
    private byte[] ppsData;

    public byte[] getSpsData() {
        return spsData;
    }

    public byte[] getPpsData() {
        return ppsData;
    }


    public static synchronized H264Utils getInstance() {
        if (h264Utils == null) {
            h264Utils = new H264Utils();
        }
        return h264Utils;
    }

    public boolean isIframe(byte[] payload) {
        int spsIndex = -1;
        int ppsIndex = -1;
        int idrIndex = -1;
        int index = 0;
        while (index + 4 < payload.length) {
            if (payload[index] == 0x00 && payload[index + 1] == 0x00 && payload[index + 2] == 0x00 && payload[index + 3] == 0x01) {
                if (payload[index + 4] == 0x67) {//Nalu type in Decimal 7 is SPS
                    // found sps NAL unit
                    spsIndex = index;
                } else if (payload[index + 4] == 0x68) {//Nalu type in Decimal 8 is PPS
                    // found pps NAL unit
                    ppsIndex = index;
                } else if (payload[index + 4] == 0x65) {//Nalu type in Decimal 5 is IDR Slice
                    //IDR NAL unit
                    idrIndex = index;
                    break;
                } else if (payload[index + 4] == 0x41) {//Nalu type in Decimal 1 is NON-IDR Slice
                    //NON-IDR Slice NAL unit
                }
            }
            index++;
        }

        if (spsIndex != -1 && ppsIndex != -1 && idrIndex != -1) {
            if (spsData == null) {
                spsData = Arrays.copyOfRange(payload, spsIndex, ppsIndex);
                ppsData = Arrays.copyOfRange(payload, ppsIndex, idrIndex);

                Log.e(TAG, "isIframe: sps " + byteArrayToHexString(spsData));
                Log.e(TAG, "isIframe: pps " + byteArrayToHexString(ppsData));
            }
            return true;
        }

        return false;
    }

    public boolean isNonIDRFrame(byte[] payloadData) {
        if (payloadData[0] == 0x00 && payloadData[1] == 0x00 && payloadData[2] == 0x00 && payloadData[3] == 0x01) {
            // Found the start of a new NAL unit
            int nalType = (payloadData[4] & 0x1F);
            if (nalType == 1) {
                Log.e(TAG, "Frame Type: P-Frame ");
                return true;
            }
        }
        return false;
    }

//    public byte[] createIFrameFromPackets(List<byte[]> iFramePackets) {
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        for (byte[] byteArray : iFramePackets) {
//            try {
//                outputStream.write(byteArray);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return outputStream.toByteArray();
//    }

    public byte[] createIFrameFromPackets(List<byte[]> iFramePackets) {
        int totalLength = 0;
        for (byte[] byteArray : iFramePackets) {
            totalLength += byteArray.length;
        }

        byte[] result = new byte[totalLength];
        int currentIndex = 0;
        for (byte[] byteArray : iFramePackets) {
            System.arraycopy(byteArray, 0, result, currentIndex, byteArray.length);
            currentIndex += byteArray.length;
        }
        return result;
    }

    /*Image into RGB Bitmap*/
    public Bitmap convertImageToBitmapRgb(Image image) {
        if (image == null) {
            return null;
        }

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer bufferY = planes[0].getBuffer();
        ByteBuffer bufferU = planes[1].getBuffer();
        ByteBuffer bufferV = planes[2].getBuffer();

        int width = image.getWidth();
        int height = image.getHeight();
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        int[] argb = new int[width * height];
        int argbIndex = 0;

        int yPos = 0;
        for (int j = 0; j < height; j++) {
            int uvPos = uvRowStride * (j >> 1);
            for (int i = 0; i < width; i++) {
                int y = bufferY.get(yPos + i) & 0xff;
                int u = bufferU.get(uvPos + (i >> 1) * uvPixelStride) & 0xff;
                int v = bufferV.get(uvPos + (i >> 1) * uvPixelStride) & 0xff;

                // YUV to RGB conversion
                int r = y + (int) (1.402f * (v - 128));
                int g = y - (int) (0.344f * (u - 128) + 0.714f * (v - 128));
                int b = y + (int) (1.772f * (u - 128));

                // Clamp values to 0-255
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                // Combine RGB channels and set pixel value
                argb[argbIndex++] = Color.rgb(r, g, b);
            }
            yPos += yRowStride;
        }

        // Create Bitmap from the pixel array
        Bitmap bitmap = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);

        // Release the Image resource
        image.close();

        return bitmap;
    }

    /*Image into ARGB Bitmap*/
    public Bitmap convertImageToBitmapArgb(Image image) {
        if (image == null) {
            return null;
        }

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer bufferY = planes[0].getBuffer();
        ByteBuffer bufferU = planes[1].getBuffer();
        ByteBuffer bufferV = planes[2].getBuffer();

        int width = image.getWidth();
        int height = image.getHeight();
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        int[] argb = new int[width * height];
        int argbIndex = 0;

        int yPos = 0;
        for (int j = 0; j < height; j++) {
            int uvPos = uvRowStride * (j >> 1);
            for (int i = 0; i < width; i++) {
                int y = bufferY.get(yPos + i) & 0xff;
                int u = bufferU.get(uvPos + (i >> 1) * uvPixelStride) & 0xff;
                int v = bufferV.get(uvPos + (i >> 1) * uvPixelStride) & 0xff;

                // YUV to RGB conversion
                int r = (int) (y + 1.402f * (v - 128));
                int g = (int) (y - 0.344f * (u - 128) - 0.714f * (v - 128));
                int b = (int) (y + 1.772f * (u - 128));

                // Clamp values to 0-255
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                // Combine RGB channels and set pixel value
                int a = 255; // Set alpha channel to 255 (opaque)
                argb[argbIndex++] = Color.argb(a, r, g, b);
            }
            yPos += yRowStride;
        }

        // Create Bitmap from the pixel array
        Bitmap bitmap = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);

        // Release the Image resource
        image.close();

        return bitmap;
    }

    /*Image into GrayScale Bitmap*/
    public Bitmap convertImageToGrayScale(Image image) {
        if (image == null) {
            return null;
        }

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();

        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();

        int pixelSize = pixelStride * width;
        int rowSize = rowStride * height;

        byte[] data = new byte[rowSize];
        buffer.get(data);

        // Create a grayscale Bitmap if the Image is in YUV_420_888 format
        if (image.getFormat() == ImageFormat.YUV_420_888) {
            int offset = 0;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    int gray = data[offset + col] & 0xFF;
                    bitmap.setPixel(col, row, 0xFF000000 | (gray << 16) | (gray << 8) | gray);
                }
                offset += rowStride;
            }

            return bitmap;
        }
        // Handle other image formats here if needed
        Log.e(TAG, "Unsupported image format: " + image.getFormat());
        return null;
    }

    private String byteArrayToHexString(byte[] byteArray) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : byteArray) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
