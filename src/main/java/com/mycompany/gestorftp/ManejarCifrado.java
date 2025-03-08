/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gestorftp;

import java.security.Key;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Mañana
 */
public class ManejarCifrado {
    
    public static Key obtenerClave (String password, int longitud) {
        Key clave = new SecretKeySpec (password.getBytes (), 0, longitud, "AES");
        return clave;
    }

    public static String cifrar (String textoEnClaro, Key key) throws Exception {       
        Cipher cipher = Cipher. getInstance ("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte [] cipherText = cipher.doFinal(textoEnClaro.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);    
    }

    public static String descifrar (String textoCifrado, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding") ;
        cipher.init(Cipher.DECRYPT_MODE, key) ;
        byte [] plainText = cipher.doFinal(Base64.getDecoder().decode(textoCifrado));
        return new String (plainText);
    }
    
    public static String formatear (String texto, int longitud) {
        
        String textoFormateado="";
        char[] letras = texto.toCharArray();
        int i=0;
        while (i<texto.length()) {
            if (i%longitud==0 && i>0) {
                while (Character.compare(letras[i], ' ')!=0) {
                    textoFormateado=textoFormateado+letras[i];
                    i++;
                }
                textoFormateado=textoFormateado+"\n";
            } else {
                textoFormateado=textoFormateado+letras[i];
            }
            i++;
        }
        return textoFormateado;
    }
}
