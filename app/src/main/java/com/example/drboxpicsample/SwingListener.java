package com.example.drboxpicsample;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

/**
 * Created by yuyanozaki on 2017/02/01.
 */

/**
 * 概要:端末投げ上げの感知システム
 */

public class SwingListener implements SensorEventListener {

    private SensorManager mSensormanager;
    private OnSwingListener mListner;
    private Sensor mAccelerometor;

    private long mPreTime;
    private float[] nValues = new float[3];//加速度センサーが取得する(x,y,z)軸の値
    private float[] oValues = {0.0f,0.0f,0.0f};

    private int mSwingCount =0;


    private static final int LI_SWING =  50;//投げ動作とみなす速度のしきい値
    private static final int CNT_SWING = 3;//投げ上げ動作の長さというか移動距離(小さな動作で反応しないようにするため)


    public SwingListener(Context context) {
        mSensormanager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    //OnSwingListener Classを使うときにOverRideの必要性を認識させるため
    public interface OnSwingListener{
        void onSwing();
    }

    //OnSwingListenerオブジェクトをセットする
    public void setOnSwingLitener(OnSwingListener listener){
        mListner = listener;
    }

    //加速度センサーを取得してリスナーに登録
    public void registSensor(){
        List<Sensor> list = mSensormanager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        //Listの中身はあるけれども、Sensorオブジェクトに何も指定されていないとき
        if(list.size()>0){
            mAccelerometor = list.get(0);
        }

        if(mAccelerometor != null ){
            /**
             * SENSOR_DELAY_GAME:センサーをどれくらいの頻度で取ってくるかの値
             */
            mSensormanager.registerListener(this,mAccelerometor,SensorManager.SENSOR_DELAY_GAME);
        }
    }


    //加速度センサーのリスナーを解除
    public void unregistSensor(){
        if(mAccelerometor != null){
            mSensormanager.unregisterListener(this);
        }
    }

    /**
     * 概要:センサーの精度が変更されると呼び出される(今回は変更されないので記述していない)
     * @param sensor
     * @param accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * 概要:センサーの値が変更されると呼び出される
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() != Sensor.TYPE_ACCELEROMETER){
            return ;
        }

        /**
         * Flow:①現在の時刻をミリ秒単位で取得
         *      ②以前計算した時間と現在の時間の差を求める
         *      ③高い頻度でイベントが発生するので100msに一回計算するように間引く
         *      ④加速度センサーの値の変化を計算し、LI_SWINGの値を上回るのか否かを判別する
         *      ⑤リスナーがセットされていればOK
         *
         */

        long curTime = System.currentTimeMillis();
        long diffTime = curTime - mPreTime;

        if(diffTime > 100){

            nValues[0] = event.values[0];
            nValues[1] = event.values[1];
            nValues[2] = event.values[2];

            float speed = (Math.abs(nValues[0]-oValues[0])+Math.abs(nValues[1]-oValues[1])+Math.abs(nValues[2]-oValues[2]))/diffTime*1000;

            if(speed > LI_SWING){//投げ上げ動作が始まったと判断する
                mSwingCount++;
                if(mSwingCount>CNT_SWING){
                    if(mListner != null){
                        mListner.onSwing();
                    }
                    mSwingCount=0;//初期化
                }
            }
            else{
                mSwingCount = 0;
            }
            mPreTime = curTime;
            oValues[0]= nValues[0];
            oValues[1]= nValues[1];
            oValues[2]= nValues[2];
        }
    }
}
