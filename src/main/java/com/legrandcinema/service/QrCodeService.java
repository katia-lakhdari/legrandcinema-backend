
package com.legrandcinema.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QrCodeService {

    private static final int TAILLE_QR_CODE = 300;

    public byte[] genererQrCode(String texte) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        try {
            BitMatrix matrice = qrCodeWriter.encode(texte, BarcodeFormat.QR_CODE, TAILLE_QR_CODE, TAILLE_QR_CODE);

            ByteArrayOutputStream fluxImage = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrice, "PNG", fluxImage);

            return fluxImage.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new RuntimeException("Erreur lors de la génération du QR code", exception);
        }
    }
}