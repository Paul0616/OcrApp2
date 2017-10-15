package ro.duoline.ocrapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class MainActivity extends AppCompatActivity {
    private ImageView poza;
    private String picturePath;
    private TextView scanResults;
    private Uri fileUri;
    private TextRecognizer detector;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final int PHOTO_REQUEST = 10;

    private static final String[] ORAS_CAUTAT = new String[]{"FOCSANI", "BUC.", "PREDEAL"};
    private Boolean[] oraseGasite = new Boolean[]{false,false,false};
    private String dataDetectata;
    private String oraDetectata;
    private String pretTotal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button);
        poza = (ImageView) findViewById(R.id.imageView);
        scanResults = (TextView) findViewById(R.id.results);
        requestStoragePermission();
        //oraseGasite = new ArrayList<Boolean>();

        detector = new TextRecognizer.Builder(getApplicationContext()).build();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanResults.setText("");
                if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if(takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        File photoFile  = null;
                        try{
                            photoFile = createImageFile();
                        } catch (IOException ex){
                            //Error
                        }
                        if (photoFile != null) {
                            fileUri = FileProvider.getUriForFile(getApplicationContext(),"ro.duoline.ocrapp",photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                            startActivityForResult(takePictureIntent, PHOTO_REQUEST);
                        }
                    } else {
                        Toast.makeText(getApplication(), "camera not supported ", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


    }

    private void requestStoragePermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return;

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_WRITE_PERMISSION){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permission NOT granted", Toast.LENGTH_LONG).show();
            }
        }
    }

    private File createImageFile() throws IOException {

        String imageFilename = "JPEG_CAMERA_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFilename, //prefix
                ".jpg", //suffix
                storageDir //directory
        );
        picturePath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PHOTO_REQUEST: {
                if (resultCode == RESULT_OK) {
                    poza.setImageURI(fileUri);

                    try {
                        Bitmap bitmap = decodeBitmapUri(this, fileUri);
                        if (detector.isOperational() && bitmap != null) {
                            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                            SparseArray<TextBlock> textBlocks = detector.detect(frame);
                            String blocks = "";
                            String lines = "";
                            String words = "";
                            for (int index = 0; index < textBlocks.size(); index++) {
                                //extract scanned text blocks here
                                TextBlock tBlock = textBlocks.valueAt(index);
                                blocks = blocks + tBlock.getValue() + "\n" + "\n";

                                for (Text line : tBlock.getComponents()) {
                                    //extract scanned text lines here
                                    lines = lines + line.getValue() + "\n";

                                    for (Text element : line.getComponents()) {
                                        //extract scanned text words here
                                        words = words + element.getValue() + "#";//+ "\n"
                                    }
                                }
                            }
                            if (textBlocks.size() == 0) {
                                scanResults.setText("Nu pot detecta text in imaginea scanata");
                            } else {
                                scanResults.setText(scanResults.getText() + "Blocks: " + "\n");
                                scanResults.setText(scanResults.getText() + blocks + "\n");
                                scanResults.setText(scanResults.getText() + "---------" + "\n");
                                scanResults.setText(scanResults.getText() + "Lines: " + "\n");
                                scanResults.setText(scanResults.getText() + lines + "\n");
                                scanResults.setText(scanResults.getText() + "---------" + "\n");

                                scanResults.setText(scanResults.getText() + "Words: " + "\n");
                                scanResults.setText(scanResults.getText() + words + "\n");
                                scanResults.setText(scanResults.getText() + "---------" + "\n");
                              //  for(int i = 0; i<ORAS_CAUTAT.length; i++){
                               //     oraseGasite.add(false);
                              //  }
                                detectezOras(words);
                                dataDetectata = "";
                                oraDetectata = "";
                                detectezDataOra(words);
                                String afisaj = "Orasul: ";
                                for(int i = 0; i<oraseGasite.length; i++){
                                    afisaj = afisaj + (oraseGasite[i] ? (ORAS_CAUTAT[i] + "\n") : "");
                                }
                                afisaj = afisaj + "Data: " + dataDetectata + "\n";
                                afisaj = afisaj + "Ora: " + oraDetectata + "\n";

                                afisaj = afisaj + "Total: " + detectezTotal(words);
                                scanResults.setText(afisaj);
                                //Toast.makeText(getBaseContext(),afisaj, Toast.LENGTH_LONG).show();


                            }
                        } else {
                            scanResults.setText("Nu se poate porni modulul OCR!");
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Nu pot incarca imaginea", Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                break;
            } // ACTION_TAKE_PHOTO_B

        }
    }

    private String detectezTotal(String words){
        //pretTotal ="";
        String pt = "";
        List<String> myList = new ArrayList<String>(Arrays.asList(words.split("#")));
        String tempMyListelement;
        //pretTotal = 0.00;

        List<Float> floatList = new ArrayList<Float>();

        for(int i = 0; i < myList.size(); i++){
            try {
                tempMyListelement = myList.get(i).toString().replace(',','.');
                tempMyListelement = tempMyListelement.replace("RON","");
                if(tempMyListelement.matches("[0-9]+(\\.[0-9][0-9]?)")){
                    floatList.add(Float.parseFloat(tempMyListelement));
                }
             //  f = Float.parseFloat(tempMyListelement);
            //    if (tempMyListelement.contains(".") && tempMyListelement.charAt(tempMyListelement.length() -1) != '.') {
             //       floatList.add(f);
            //    }

            } catch (NumberFormatException nfe){
                nfe.printStackTrace();
            }

        }
        if(floatList.size() > 0) {
            float pretTotal = floatList.get(0);
            for(Float fl: floatList){
                if(fl > pretTotal) {
                    pt = Float.toString(fl);
                }
            }


        }
        return pt;

    }
    private void detectezOras(String words){
        List<String> myList = new ArrayList<String>(Arrays.asList(words.split("#")));
       // iesire:

        for(int i = 0; i<myList.size();i++) {
            for (int j = 0; j < ORAS_CAUTAT.length; j++) {
                if (sirAcceptabil(myList.get(i).toString(), ORAS_CAUTAT[j])){
                    //Toast.makeText(getBaseContext(),"Am detectat orasul: " + ORAS_CAUTAT[j], Toast.LENGTH_LONG).show();
                    //break iesire;
                    oraseGasite[j] = true;
                }
            }
        }

    }
    public void detectezDataOra(String words){
        List<String> myList = new ArrayList<String>(Arrays.asList(words.split("#")));
        for(int i = 0; i<myList.size();i++) {
            if (myList.get(i).matches("(0[1-9]|1[0-9]|2[0-9]|3[01]).(0[1-9]|1[012]).[0-9]{4}")){
                dataDetectata = myList.get(i).toString();
            }
        }
        for(int i = 0; i<myList.size();i++) {
            if (myList.get(i).matches("(0[1-9]|1[0-9]|2[0-3])(:[0-5][0-9])")){
                oraDetectata = myList.get(i).toString();
            }
        }
        for(int i = 0; i<myList.size();i++) {
            if (myList.get(i).matches("(0[0-9]|1[0-9]|2[0-3])(:[0-5][0-9]){2}")){
                oraDetectata = myList.get(i).toString();
            }
        }

    }

    private Boolean sirAcceptabil(String sirDetectat, String orasEtalon){ //reintoarce true daca diferenta dintre siruri e acceptabila si se considera potrivire si false daca diferenta e prea mare
        int lungimeDetectat = sirDetectat.length();
        int lungimeEtalon = orasEtalon.length();
        int diferentaDeLungime = Integer.max(lungimeDetectat, lungimeEtalon) - Integer.min(lungimeDetectat,lungimeEtalon);
        if(diferentaDeLungime > 1){
            return false; //diferenta de lungime intre siruri mai mare decat 1: potrivire inacceptabila
        } else {
            int z = compareStrings(sirDetectat.toUpperCase().replaceAll("\\s",""), orasEtalon); //numarul de litere care difera intre cele 2 cuvinte
            if(z > 2) {
                return false;
            }
            else {
                if(lungimeDetectat > 3) {
                    return true;
                } else {
                    return false;
                }
            }
        }

    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }

    private int compareStrings(String s1, String s2){
        int counter = 0;
        char[] first = s1.toLowerCase().toCharArray();
        char[] second = s2.toLowerCase().toCharArray();

        int minLenght = Math.min(first.length, second.length);

        for (int i = 0; i <minLenght; i++){
            if(first[i] != second[i]){
                counter++;
            }

        }
        if (first.length == 1){
            return 0;
        } else return counter;
    }
}
