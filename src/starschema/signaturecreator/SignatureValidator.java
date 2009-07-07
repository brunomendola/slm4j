/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starschema.signaturecreator;

import java.io.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

/**
 *
 * @author User
 */
public class SignatureValidator {

    private PublicKey publicKey;
    private Signature signature;
    private byte[] licenseSignature;
    private static final String LICENSE_BEGIN = "----- BEGIN LICENSE -----";
    private static final String LICENSE_END = "----- END LICENSE -----";
    private static final String SIGNATURE_BEGIN = "----- BEGIN SIGNATURE -----";
    private static final String SIGNATURE_END = "----- END SIGNATURE -----";
    private static final int SIGNATURE_LINE_LENGTH = 20;

    private void initializeSignatureVerify() throws LicenseGeneratorException {
        try {
            signature = Signature.getInstance("SHA1withDSA");
            signature.initVerify(publicKey);
        } catch (Exception ex) {
            throw new LicenseGeneratorException("Error in initializing signature for verification ( " + ex.getMessage() + " )");
        }
    }

    private void processSourceFile(String sourceFile) throws LicenseGeneratorException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(sourceFile);
            bufferedReader = new BufferedReader(fileReader);
            boolean isLicense = true;
            String line;

            isLicense = false;

            while (bufferedReader.ready() && !isLicense) {
                line = bufferedReader.readLine();
                if (line.equals(LICENSE_BEGIN)) {
                    isLicense = true;
                }
            }

            while (bufferedReader.ready() && isLicense) {
                line = bufferedReader.readLine();
                if (!line.equals(LICENSE_END)) {
                    signature.update(line.getBytes(), 0, line.getBytes().length);
                } else {
                    isLicense = false;
                }
            }

            licenseSignature = readSignature(bufferedReader);
        } catch (Exception ex) {
            throw new LicenseGeneratorException("Error in processing source file ( " + ex.getMessage() + " )");
        } finally {
            try {
                bufferedReader.close();
                fileReader.close();
            } catch (Exception ex) {
            }
        }
    }

    private boolean readPublicKey(String publicKeyFile) throws LicenseGeneratorException {
        FileReader fileReader = null;
        BufferedReader bufferedReader;

        try {
            if (!new File(publicKeyFile).exists()) {
                return false;
            }

            String publicKeyString = "";

            fileReader = new FileReader(publicKeyFile);
            bufferedReader = new BufferedReader(fileReader);

            while (bufferedReader.ready()) {
                publicKeyString += bufferedReader.readLine();
            }

            publicKey = KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(Base64Coder.decode(publicKeyString)));

            return true;
        } catch (Exception ex) {
            throw new LicenseGeneratorException("Error in reading public key from file " + publicKeyFile + " ( " + ex.getMessage() + " )");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }
    }

    private byte[] readSignature(BufferedReader bufferedReader) throws LicenseGeneratorException {
        try {
            boolean isSignature = false;
            String line;
            String signatureString = new String();


            while (bufferedReader.ready() && !isSignature) {
                line = bufferedReader.readLine();
                if (line.equals(SIGNATURE_BEGIN)) {
                    isSignature = true;
                }
            }

            while (bufferedReader.ready() && isSignature) {
                line = bufferedReader.readLine();
                if (line.equals(SIGNATURE_END)) {
                    isSignature = false;
                } else {
                    signatureString += line;
                }
            }

            return Base64Coder.decode(signatureString);
        } catch (Exception ex) {
            throw new LicenseGeneratorException("Error in reading signature from file ( " + ex.getMessage() + " )");
        }
    }

    private boolean verifySignature() throws LicenseGeneratorException {
        try {
            return signature.verify(licenseSignature);
        } catch (Exception ex) {
            throw new LicenseGeneratorException("Error in verification ( " + ex.getMessage() + " )");
        }
    }

    public boolean verifyLicense(String publicKeyFile, String signatureFile) throws Exception {
        try {
            readPublicKey(publicKeyFile);
            initializeSignatureVerify();
            processSourceFile(signatureFile);

            if (verifySignature()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            throw new LicenseGeneratorException("Error in signature verification ( " + ex.getMessage() + " )");
        }
    }

    public boolean verifyLicenseWithString(String publicKeyString, String signatureFile) throws Exception {
        try {
            publicKey = KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(Base64Coder.decode(publicKeyString)));

            initializeSignatureVerify();
            processSourceFile(signatureFile);

            if (verifySignature()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            throw new LicenseGeneratorException("Error in signature verification ( " + ex.getMessage() + " )");
        }
    }
}
