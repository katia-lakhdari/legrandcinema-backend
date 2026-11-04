package com.legrandcinema.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeServiceTest {

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService();
    }

    @Test
    void genererQrCode_renvoieUneImagePng() {
        byte[] imageQrCode = qrCodeService.genererQrCode("billet-test");

        assertNotNull(imageQrCode);
        assertTrue(imageQrCode.length > 0);
        assertEquals((byte) 0x89, imageQrCode[0]);
        assertEquals((byte) 'P', imageQrCode[1]);
        assertEquals((byte) 'N', imageQrCode[2]);
        assertEquals((byte) 'G', imageQrCode[3]);
    }

    @Test
    void genererQrCode_contientLeTexteDuBillet() throws Exception {
        String codeBillet = UUID.randomUUID().toString();

        byte[] imageQrCode = qrCodeService.genererQrCode(codeBillet);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageQrCode));
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result resultat = new MultiFormatReader().decode(bitmap);

        assertEquals(codeBillet, resultat.getText());
    }
}