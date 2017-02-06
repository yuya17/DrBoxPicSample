package com.example.drboxpicsample;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.R.attr.data;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static android.os.Build.VERSION_CODES.N;
import static com.example.drboxpicsample.R.string.logout;

/**
 * DropBoxへのログイン処理とカメラ撮影
 */
public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final String APP_KEY = "mlgt6jh3cbozuwu";
    private static final String APP_SECRET = "xmtx5qv47cpd4tv";
    private static final String TAG = "DrBoxPicSample";

    private DropboxAPI<AndroidAuthSession> mApi;
    private boolean mLoggedIn = false;


    private Button mTakePicture;
    private Button mSubmit;
    private ImageView  mImage;

    private static final int NEW_PICTURE = 1;
    private String mCameraFileName;
    private File mFile = null;

    private final String PHOTO_DIR = "/Photos/";
    private SwingListener mSwingListener;




    /**
     * 認証周りの動作を行う
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY,APP_SECRET);

        /**Session:コンピュータシステムやネットワーク通信において、
         *         接続/ログインしてから、切断/ログオフするまでの一連の操作や通信のこと。*/
        //つまり、appKeyPairの情報を受け取り接続から切断まで行いますよってこと
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);

        //DropboxAPIクラスにsessionを渡す。
        mApi = new DropboxAPI<AndroidAuthSession>(session);

        //接続状況を確認してからViewを表示している
        setContentView(R.layout.activity_main);

        checkWriteExternalPermission();

        mSubmit = (Button) findViewById(R.id.submit);

        /**
         *概要： ログインボタンを押した時の処理を記載
         */

        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLoggedIn){
                    logout();
                    mSubmit.setText(R.string.login);
                }
                else{
                    /**
                     * startOAuth2Authentication()の引数にMainActivityを指定しているので、
                     * コールバックでこのアクビティに戻ってくる。
                     * コールバックされた時の記述はonResume()に記載している。
                     */
                    mApi.getSession().startOAuth2Authentication(MainActivity.this);
                }
            }
        });

        mImage = (ImageView) findViewById(R.id.image_view);
        mTakePicture = (Button) findViewById(R.id.photo_button);

        /**
         * 概要:写真をとるボタンが押された時の処理を記述
         */

        mTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //暗黙的Intent
                Intent intent = new Intent();
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                Date date = new Date();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss", Locale.JAPAN);

                String newPicFile =dateFormat.format(date) + ".jpg";
                /**
                 * Environment Class:データ／キャッシュ／外部ストレージ／システムのパスを取得するのによく使うClass
                 * newPicFileのデータが入ってるPathを取得
                 */
                String outPath = new File(Environment.getExternalStorageDirectory(),newPicFile).getPath();
                //outpathにあるFileを取得
                File outfile = new File(outPath);

                mCameraFileName = outfile.toString();

                /**
                 * 概要： 画像ファイルをAndroid端末に返してもらうURIをセットする
                 */
                Uri outuri = Uri.fromFile(outfile);
                //遷移先の情報をLog出力する。
                Log.i(TAG,"uri"+ outuri);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,outuri);
                Log.i(TAG,"Importing new Picture" + mCameraFileName);
                try {
                    //NEW_PICTUREのリクエストコードでこのIntentを引き渡す。
                    /**
                     * startActivity(Intent intent):開くアクティビティに対して何かしらの情報を与える、
                     * startActivityForResult(Intent intent, int requestCode):開いたアクティビティから何かしらの情報を受け取ることを可能とする。
                     * つまり、今回はDeviceとDropBox間のデータのやり取りなので、startActivityForResult()を使う。
                     */
                    startActivityForResult(intent, NEW_PICTURE);
                }
                catch (ActivityNotFoundException e){
                    Toast.makeText(MainActivity.this,"There doesn't seem to be a camera",Toast.LENGTH_LONG).show();
                }
            }
        });


        mSwingListener = new SwingListener(this);
        mSwingListener.setOnSwingLitener(new SwingListener.OnSwingListener() {
            @Override
            public void onSwing() {
                if (mFile != null) {
                    UploadPicture upload = new UploadPicture(MainActivity.this, mApi, PHOTO_DIR, mFile);
                    upload.execute();
                }
            }
        });

        mSwingListener.registSensor();


    }

    /**
     * 概要：メンバ変数が初期化されてしまうことへの対処
     * onSaveInstanceState()でBundleオブジェクトにメンバ変数の値をpushするメソッド
     * onRestoreinstanceState()でBundleオブジェクトからgetメソッドで呼び出す。
     * @param outState
     */
    /**
     * Bundle Class:OSの判断で強制的に停止、終了する時に一時的にデータを格納するクラス
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //keyを設定し、mCameraFileNameのpathに文章をセッティング
        outState.putString("mCameraFileName",mCameraFileName);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCameraFileName = savedInstanceState.getString("mCameraFileName");
    }

    /**
     * 概要:コールバックされた時の処理を記述
     * ①認証処理を完了すること
     * ②ユーザ識別情報がSessionに関連付けされる
     * ③ユーザー識別情報を取得
     */
    @Override
    protected void onResume() {
        super.onResume();

        AndroidAuthSession session = mApi.getSession();

        /**
         * authenticationSuccessful():受け取ったSessionの情報の中にアクセストークンの情報が
         *                             入ってるかどうかを判別している。
         */
        if(session.authenticationSuccessful()){
            //情報が入っているので、認証処理を終了
           try {
               session.finishAuthentication();
               //sessionの中に入ってるTokenを取得
               String accessToken = session.getOAuth2AccessToken();
               mLoggedIn = true;
               mSubmit.setText(R.string.logout);
           }
           catch(IllegalStateException e){
               showToast("Couldn't authenticate with Dropbox: " +e.getLocalizedMessage());
               Log.i(TAG,"Error authientiathig",e);
           }

        }

    }

    /**
     * 概要：起動先のActivity (ここで言うと、MediaStore)から結果を送り返してもらうメソッド
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = null;
                if (data != null) {
                    uri = data.getData();
                }
                if (uri == null && mCameraFileName != null) {
                    uri = Uri.fromFile(new File(mCameraFileName));
                }

                /**
                 * Bitmap Class: 画像ファイルの表示や変更などに使うクラス
                 * 利点：読み込む画像に対しての制限などが行えるため,場合に応じてサイズを変更できる。
                 */
                Bitmap bitmap = BitmapFactory.decodeFile(mCameraFileName);
                mImage.setImageBitmap(bitmap);
                //DropBoxにuploadするためのファイルを作成する。
                mFile = new File(mCameraFileName);

                if (uri != null) {
                    Toast.makeText(MainActivity.this, "投げ上げ動作で,DropBoxに写真をuploadする", Toast.LENGTH_LONG).show();
                }


            } else {//Errorを表示
                Log.w(TAG, "Unknown Activity Result from mediaImport:" + requestCode);
            }
        }
    }

    //Sessionからcredentialを除く
    private void logout(){
        mApi.getSession().unlink();
        mLoggedIn = false;
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    private void checkWriteExternalPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                //一度拒否された時、Rationale（理論的根拠）を説明して、再度許可ダイアログを出すようにする
                new AlertDialog.Builder(this)
                        .setTitle("許可が必要です")
                        .setMessage("ファイルを保存してアップロードするために、WRITE_EXTERNAL_STOREAGEを許可してください")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // OK button pressed
                                requestWriteExternalStorage();
                            }
                        })
                        .setNegativeButton("Cancel",  new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showToast("外部へのファイルの保存が許可されなかったので、画像を保存できません");
                            }
                        })
                        .show();
            } else {
                // まだ許可を求める前の時、許可を求めるダイアログを表示します。
                requestWriteExternalStorage();
            }
        }
    }
    private void requestWriteExternalStorage() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // ユーザーが許可したとき
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("これで画像をアップロードできます");
                }
                else {
                    // ユーザーが許可しなかったとき
                    // 許可されなかったため機能が実行できないことを表示する
                    showToast("外部へのファイルの保存が許可されなかったので、画像を保存できません");
                }
                return;
            }
        }
    }

}
