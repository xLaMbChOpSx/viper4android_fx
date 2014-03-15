
package com.vipercn.viper4android_v2.service;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.vipercn.viper4android_v2.activity.IRSUtils;
import com.vipercn.viper4android_v2.activity.Utils;
import com.vipercn.viper4android_v2.activity.V4AJniInterface;
import com.vipercn.viper4android_v2.activity.ViPER4Android;

public class ViPER4AndroidService extends Service {
    protected static final String TAG = ViPER4AndroidService.class.getSimpleName();
    public static final UUID ID_V4A_GENERAL_FX = UUID
            .fromString("41d3c987-e6cf-11e3-a88a-11aba5d5c51b");

    private class ResourceMutex {

        private final Semaphore mSignal = new Semaphore(1);

        public boolean acquire() {
            try {
                mSignal.acquire();
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }

        public void release() {
            mSignal.release();
        }
    }

    protected class V4ADSPModule {

        public AudioEffect mInstance;

        public V4ADSPModule(int nAudioSession) {
            try {
                Log.i(TAG, "Creating viper4android module");
                mInstance = new AudioEffect(AudioEffect.EFFECT_TYPE_NULL, ID_V4A_GENERAL_FX, 0,
                        nAudioSession);

                AudioEffect.Descriptor adModuleDescriptor = mInstance.getDescriptor();
                Log.i(TAG, "Effect name : " + adModuleDescriptor.name);
                Log.i(TAG, "Type id : " + adModuleDescriptor.type.toString());
                Log.i(TAG, "Unique id : " + adModuleDescriptor.uuid.toString());
                Log.i(TAG, "Implementer : " + adModuleDescriptor.implementor);
                Log.i(TAG, "Connect mode : " + adModuleDescriptor.connectMode);

            } catch (IllegalArgumentException e) {
                Log.e(TAG,
                        "Can not create audio effect instance, V4A driver not installed or not supported by this rom");
            } catch (UnsupportedOperationException e) {
                Log.e(TAG,
                        "Can not create audio effect instance, V4A driver not installed or not supported by this rom");
            } catch (Exception e) {
                Log.e(TAG,
                        "Can not create audio effect instance, V4A driver not installed or not supported by this rom");
            }
        }

        protected void release() {
            Log.i(TAG, "Free viper4android module.");
            if (mInstance != null) {
                try {
                    mInstance.release();
                } catch (Exception e) {
                    Log.e(TAG, "release() " + e.getMessage());
                }
            }
        }

        public void setParameter(int param, int valueL) {
            try {
                setParameter(mInstance.intToByteArray(param), mInstance.intToByteArray(valueL));
            } catch (Exception e) {
                Log.i(TAG, "setParameter_px4_vx4x1: " + e.getMessage());
            }
        }

        public void setParameter(int param, int valueL, int valueH) {
            try {
                byte[] p = mInstance.intToByteArray(param);
                byte[] vL = mInstance.intToByteArray(valueL);
                byte[] vH = mInstance.intToByteArray(valueH);
                byte[] v = mInstance.concatArrays(vL, vH);
                setParameter(p, v);
            } catch (Exception e) {
                Log.i(TAG, "setParameter_px4_vx4x2: " + e.getMessage());
            }
        }

        public void setParameter(int param, int valueL, int valueH, int valueE) {
            try {
                byte[] p = mInstance.intToByteArray(param);
                byte[] vL = mInstance.intToByteArray(valueL);
                byte[] vH = mInstance.intToByteArray(valueH);
                byte[] vE = mInstance.intToByteArray(valueE);
                byte[] v = mInstance.concatArrays(vL, vH, vE);
                setParameter(p, v);
            } catch (Exception e) {
                Log.i(TAG, "setParameter_px4_vx4x3: " + e.getMessage());
            }
        }

        @SuppressWarnings("unused")
        /* For future use */
        public void setParameter(int param, int valueL, int valueH, int valueE, int valueR) {
            try {
                byte[] p = mInstance.intToByteArray(param);
                byte[] vL = mInstance.intToByteArray(valueL);
                byte[] vH = mInstance.intToByteArray(valueH);
                byte[] vE = mInstance.intToByteArray(valueE);
                byte[] vR = mInstance.intToByteArray(valueR);
                byte[] v = mInstance.concatArrays(vL, vH, vE, vR);
                setParameter(p, v);
            } catch (Exception e) {
                Log.i(TAG, "setParameter_px4_vx4x4: " + e.getMessage());
            }
        }

        public void setParameter(int param, int dataLength, byte[] byteData) {
            try {
                byte[] p = mInstance.intToByteArray(param);
                byte[] vL = mInstance.intToByteArray(dataLength);
                byte[] v = mInstance.concatArrays(vL, byteData);
                if (v.length < 256) {
                    int zeroPad = 256 - v.length;
                    byte[] zeroArray = new byte[zeroPad];
                    v = mInstance.concatArrays(v, zeroArray);
                }
                setParameter(p, v);
            } catch (Exception e) {
                Log.i(TAG, "setParameter_px4_vx1x256: " + e.getMessage());
            }
        }

        public void setParameter(int param, int valueL, int dataLength, byte[] byteData) {
            try {
                byte[] p = mInstance.intToByteArray(param);
                byte[] vL = mInstance.intToByteArray(valueL);
                byte[] vH = mInstance.intToByteArray(dataLength);
                byte[] v = mInstance.concatArrays(vL, vH, byteData);
                if (v.length < 8192) {
                    int zeroPad = 8192 - v.length;
                    byte[] zeroArray = new byte[zeroPad];
                    v = mInstance.concatArrays(v, zeroArray);
                }
                setParameter(p, v);
            } catch (Exception e) {
                Log.i(TAG, "setParameter_px4_vx2x8192: " + e.getMessage());
            }
        }

        @SuppressWarnings("unused")
        public void setParameter(int param, String szData) {
            int stringLen = szData.length();
            byte[] stringBytes = szData.getBytes(Charset.forName("US-ASCII"));
            setParameter(param, stringLen, stringBytes);
        }

        public void setParameter(byte[] parameter, byte[] value) {
            if (mInstance == null)
                return;
            try {
                Method setParameter = AudioEffect.class.getMethod("setParameter", byte[].class,
                        byte[].class);
                setParameter.invoke(mInstance, parameter, value);
            } catch (Exception e) {
                Log.i(TAG, "setParameter_Native: " + e.getMessage());
            }
        }

        public int getParameter_px4_vx4x1(int param) {
            try {
                byte[] p = mInstance.intToByteArray(param);
                byte[] v = new byte[4];
                getParameter_Native(p, v);
                return mInstance.byteArrayToInt(v, 0);
            } catch (Exception e) {
                Log.i(TAG, "getParameter_px4_vx4x1: " + e.getMessage());
                return -1;
            }
        }

        public void getParameter_Native(byte[] parameter, byte[] value) {
            if (mInstance == null)
                return;
            try {
                Method getParameter = AudioEffect.class.getMethod("getParameter", byte[].class,
                        byte[].class);
                getParameter.invoke(mInstance, parameter, value);
            } catch (Exception e) {
                Log.i(TAG, "getParameter_Native: " + e.getMessage());
            }
        }

        private void proceedIRBuffer_Speaker(String mConvIRFile, int mChannels, int mFrames) {
            // 1. Tell driver to prepare kernel buffer
            Random rndMachine = new Random();
            int mKernelBufferID = rndMachine.nextInt();
            setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER,
                    mKernelBufferID, mChannels, 0);
            // 2. Read entire ir data and get hash code
            byte[] mKernelData = V4AJniInterface.ReadImpulseResponseToArray(mConvIRFile);
            // Read failed or Empty ir file
            if (mKernelData == null || mKernelData.length <= 0) {
                setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }

            int[] mHashCode = V4AJniInterface.GetHashImpulseResponseArray(mKernelData);
            // No hash or wrong hash
            if (mHashCode == null || mHashCode.length != 2 || mHashCode[0] == 0) {
                setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }

            int hashCode = mHashCode[1];

            Log.i("ViPER4Android", "[Kernel] Channels = " + mChannels + ", Frames = " + mFrames
                    + ", Bytes = " + mKernelData.length + ", Hash = " + hashCode);

            // 3. Split kernel data and send to driver
            /*
             * 8192(packet size) - sizeof(int) - sizeof(int), 8184 bytes = 2046
             * float samples = 1023 stereo frames
             */
            int mBlockSize = 8184;
            int mRestBytes = mKernelData.length, mSendOffset = 0;
            while (mRestBytes > 0) {
                int minBlockSize = Math.min(mBlockSize, mRestBytes);
                byte[] mSendData = new byte[minBlockSize];
                System.arraycopy(mKernelData, mSendOffset, mSendData, 0, minBlockSize);
                mSendOffset += minBlockSize;
                mRestBytes -= minBlockSize;
                // Send to driver
                int mFramesCount = minBlockSize / 4; /* sizeof(float) = 4 */
                setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_SETBUFFER,
                        mKernelBufferID, mFramesCount, mSendData);
            }

            // 4. Tell driver to commit kernel buffer
            byte[] mIrsName = mConvIRFile.getBytes();
            int mIrsNameHashCode = 0;
            if (mIrsName != null) {
                mIrsNameHashCode = (int) IRSUtils.hashIRS(mIrsName, mIrsName.length);
            }
            setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_COMMITBUFFER,
                    mKernelBufferID, hashCode, mIrsNameHashCode);
        }

        private void proceedIRBuffer_Headphone(String mConvIRFile, int mChannels, int mFrames) {
            // 1. Tell driver to prepare kernel buffer
            Random rndMachine = new Random();
            int mKernelBufferID = rndMachine.nextInt();
            setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, mKernelBufferID,
                    mChannels, 0);

            // 2. Read entire ir data and get hash code
            byte[] mKernelData = V4AJniInterface.ReadImpulseResponseToArray(mConvIRFile);
            // Read failed or Empty ir file
            if (mKernelData == null || mKernelData.length <= 0) {
                setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            int[] mHashCode = V4AJniInterface.GetHashImpulseResponseArray(mKernelData);
            // No hash or wrong hash
            if (mHashCode == null || mHashCode.length != 2 || mHashCode[0] == 0) {
                setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            int hashCode = mHashCode[1];

            Log.i("ViPER4Android", "[Kernel] Channels = " + mChannels + ", Frames = " + mFrames
                    + ", Bytes = " + mKernelData.length + ", Hash = " + hashCode);

            // 3. Split kernel data and send to driver
            /*
             * 8192(packet size) - sizeof(int) - sizeof(int), 8184 bytes = 2046
             * float samples = 1023 stereo frames
             */
            int mBlockSize = 8184;
            int mRestBytes = mKernelData.length, mSendOffset = 0, mPacketIndex = 0;
            while (mRestBytes > 0) {
                int minBlockSize = Math.min(mBlockSize, mRestBytes);
                byte[] mSendData = new byte[minBlockSize];
                System.arraycopy(mKernelData, mSendOffset, mSendData, 0, minBlockSize);
                mSendOffset += minBlockSize;
                mRestBytes -= minBlockSize;
                Log.i("ViPER4Android", "Setting kernel buffer, index = " + mPacketIndex
                        + ", length = " + minBlockSize);
                mPacketIndex++;
                // Send to driver
                int mFramesCount = minBlockSize / 4; /* sizeof(float) = 4 */
                setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_SETBUFFER,
                        mKernelBufferID, mFramesCount, mSendData);
            }

            // 4. Tell driver to commit kernel buffer
            byte[] mIrsName = mConvIRFile.getBytes();
            int mIrsNameHashCode = 0;
            if (mIrsName != null) {
                mIrsNameHashCode = (int) IRSUtils.hashIRS(mIrsName, mIrsName.length);
            }
            setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_COMMITBUFFER,
                    mKernelBufferID, hashCode, mIrsNameHashCode);
        }

        private void proceedIRBuffer_Speaker(IRSUtils irs, String mConvIRFile) {
            // 1. Tell driver to prepare kernel buffer
            Random rndMachine = new Random();
            int mKernelBufferID = rndMachine.nextInt();
            setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER,
                    mKernelBufferID, irs.getChannels(), 0);

            // 2. Read entire ir data and get hash code
            byte[] mKernelData = irs.readEntireData();
            // Read failed or Empty ir file
            if (mKernelData == null || mKernelData.length <= 0) {
                setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            long mHashCode = IRSUtils.hashIRS(mKernelData, mKernelData.length);
            int hashCode = (int) (long) mHashCode;

            Log.i("ViPER4Android",
                    "[Kernel] Channels = " + irs.getChannels() + ", Frames = "
                            + irs.getSampleCount()
                            + ", Bytes = " + mKernelData.length + ", Hash = " + hashCode);

            // 3. Split kernel data and send to driver
            /*
             * 8192(packet size) - sizeof(int) - sizeof(int), 8184 bytes = 2046
             * float samples = 1023 stereo frames
             */
            int mBlockSize = 8184;
            int mRestBytes = mKernelData.length, mSendOffset = 0;
            while (mRestBytes > 0) {
                int minBlockSize = Math.min(mBlockSize, mRestBytes);
                byte[] mSendData = new byte[minBlockSize];
                System.arraycopy(mKernelData, mSendOffset, mSendData, 0, minBlockSize);
                mSendOffset += minBlockSize;
                mRestBytes -= minBlockSize;
                // Send to driver
                int mFramesCount = minBlockSize / 4; /* sizeof(float) = 4 */
                setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_SETBUFFER,
                        mKernelBufferID, mFramesCount, mSendData);
            }

            // 4. Tell driver to commit kernel buffer
            byte[] mIrsName = mConvIRFile.getBytes();
            int mIrsNameHashCode = 0;
            if (mIrsName != null) {
                mIrsNameHashCode = (int) IRSUtils.hashIRS(mIrsName, mIrsName.length);
            }
            setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_COMMITBUFFER,
                    mKernelBufferID, hashCode, mIrsNameHashCode);
        }

        private void proceedIRBuffer_Headphone(IRSUtils irs, String mConvIRFile) {
            // 1. Tell driver to prepare kernel buffer
            Random rndMachine = new Random();
            int mKernelBufferID = rndMachine.nextInt();
            setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER,
                    mKernelBufferID, irs.getChannels(), 0);

            // 2. Read entire ir data and get hash code
            byte[] mKernelData = irs.readEntireData();
            // Read failed or Empty ir file
            if (mKernelData == null || mKernelData.length <= 0) {
                setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                return;
            }
            long nlHashCode = IRSUtils.hashIRS(mKernelData, mKernelData.length);
            int hashCode = (int) (long) nlHashCode;

            Log.i("ViPER4Android",
                    "[Kernel] Channels = " + irs.getChannels() + ", Frames = "
                            + irs.getSampleCount()
                            + ", Bytes = " + mKernelData.length + ", Hash = " + hashCode);

            // 3. Split kernel data and send to driver
            /*
             * 8192(packet size) - sizeof(int) - sizeof(int), 8184 bytes = 2046
             * float samples = 1023 stereo frames
             */
            int mBlockSize = 8184;
            int mRestBytes = mKernelData.length, mSendOffset = 0, mPacketIndex = 0;
            while (mRestBytes > 0) {
                int minBlockSize = Math.min(mBlockSize, mRestBytes);
                byte[] mSendData = new byte[minBlockSize];
                System.arraycopy(mKernelData, mSendOffset, mSendData, 0, minBlockSize);
                mSendOffset += minBlockSize;
                mRestBytes -= minBlockSize;
                Log.i("ViPER4Android", "Setting kernel buffer, index = " + mPacketIndex + "," +
                        "length = " + minBlockSize);
                mPacketIndex++;
                // Send to driver
                int mFramesCount = minBlockSize / 4; /* sizeof(float) = 4 */
                setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_SETBUFFER,
                        mKernelBufferID, mFramesCount, mSendData);
            }

            // 4. Tell driver to commit kernel buffer
            byte[] mIrsName = mConvIRFile.getBytes();
            int mIrsNameHashCode = 0;
            if (mIrsName != null) {
                mIrsNameHashCode = (int) IRSUtils.hashIRS(mIrsName, mIrsName.length);
            }
            setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_COMMITBUFFER,
                    mKernelBufferID, hashCode, mIrsNameHashCode);
        }

        public void setConvIRFile(String mConvIRFile, boolean bSpeakerParam) {
            /* Commit irs when called here */

            // Check irs file
            File chkIrsFile = new File(mConvIRFile);
            if (!chkIrsFile.exists()) {
                Log.i("ViPER4Android", "Convolver kernel does not exist");
                // Clear convolver ir file
                if (bSpeakerParam) {
                    setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                } else {
                    setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                }
                return;
            } else {
                Log.i("ViPER4Android", "Convolver kernel exists");
            }

            if (mConvIRFile == null) {
                Log.i("ViPER4Android", "Clear convolver kernel");
                // Clear convolver ir file
                if (bSpeakerParam) {
                    setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                } else {
                    setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                }
            } else {
                Log.i("ViPER4Android", "Convolver kernel = " + mConvIRFile);

                // Set convolver ir file
                if (mConvIRFile.equals("")) {
                    Log.i("ViPER4Android", "Clear convolver kernel");
                    // Clear convolver ir file
                    if (bSpeakerParam) {
                        setParameter(ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER, 0, 0, 1);
                    } else {
                        setParameter(ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER, 0, 0, 1);
                    }
                } else {
                    boolean needSendIRSToDriver = true;
                    byte[] mIrsName = mConvIRFile.getBytes();
                    if (mIrsName != null) {
                        long mIrsNameHash = IRSUtils.hashIRS(mIrsName, mIrsName.length);
                        int irsNameHash = (int) mIrsNameHash;
                        int irsNameHashInDriver = getParameter_px4_vx4x1(PARAM_GET_CONVKNLID);
                        Log.i("ViPER4Android", "Kernel ID = [driver: " + irsNameHashInDriver
                                + ", client: " + irsNameHash + "]");
                        if (irsNameHash == irsNameHashInDriver) {
                            needSendIRSToDriver = false;
                        }
                    }

                    if (!needSendIRSToDriver) {
                        Log.i("ViPER4Android", "Driver is holding the same irs now");
                        return;
                    }

                    int mCommand = ViPER4AndroidService.PARAM_HPFX_CONV_PREPAREBUFFER;
                    if (bSpeakerParam) {
                        mCommand = ViPER4AndroidService.PARAM_SPKFX_CONV_PREPAREBUFFER;
                    }

                    Log.i("ViPER4Android", "We are going to load irs through internal method");
                    IRSUtils irs = new IRSUtils();
                    if (irs.LoadIrs(mConvIRFile)) {
                        /* Proceed buffer */
                        if (bSpeakerParam) {
                            proceedIRBuffer_Speaker(irs, mConvIRFile);
                        } else {
                            proceedIRBuffer_Headphone(irs, mConvIRFile);
                        }
                        irs.Release();
                    } else {
                        if (V4AJniInterface.IsLibraryUsable()) {
                            Log.i("ViPER4Android", "We are going to load irs through jni");
                            // Get ir file info
                            int[] iaIRInfo = V4AJniInterface
                                    .GetImpulseResponseInfoArray(mConvIRFile);
                            if (iaIRInfo == null) {
                                setParameter(mCommand, 0, 0, 1);
                            } else {
                                if (iaIRInfo.length != 4) {
                                    setParameter(mCommand, 0, 0, 1);
                                } else {
                                    if (iaIRInfo[0] == 0) {
                                        setParameter(mCommand, 0, 0, 1);
                                    } else {
                                        /* Proceed buffer */
                                        if (bSpeakerParam) {
                                            proceedIRBuffer_Speaker(mConvIRFile, iaIRInfo[1],
                                                    iaIRInfo[2]);
                                        } else {
                                            proceedIRBuffer_Headphone(mConvIRFile, iaIRInfo[1],
                                                    iaIRInfo[2]);
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.i("ViPER4Android", "Failed to load " + mConvIRFile);
                        }
                    }
                }
            }
        }
    }

    public class LocalBinder extends Binder {

        public ViPER4AndroidService getService() {
            return ViPER4AndroidService.this;
        }
    }

    private static final int DEVICE_GLOBAL_OUTPUT_MIXER = 0;

    /* ViPER4Android Driver Status */
    private static final int PARAM_GET_NEONENABLED = 32770;
    private static final int PARAM_GET_ENABLED = 32771;
    private static final int PARAM_GET_CONFIGURE = 32772;
    private static final int PARAM_GET_DRVCANWORK = 32773;
    private static final int PARAM_GET_STREAMING = 32774;
    @SuppressWarnings("unused")
    private static final int PARAM_GET_EFFECT_TYPE = 32775;
    private static final int PARAM_GET_SAMPLINGRATE = 32776;
    private static final int PARAM_GET_CONVUSABLE = 32777;
    private static final int PARAM_GET_CONVKNLID = 32778;

    /* ViPER4Android Driver Status Control */
    private static final int PARAM_SET_UPDATE_STATUS = 36866;
    private static final int PARAM_SET_RESET_STATUS = 36867;
    private static final int PARAM_SET_DOPROCESS_STATUS = 36868;
    private static final int PARAM_SET_FORCEENABLE_STATUS = 36869;
    private static final int PARAM_SET_SELFDIAGNOSE_STATUS = 36870;

    /* ViPER4Android FX Types */
    @SuppressWarnings("unused")
    private static final int V4A_FX_TYPE_NONE = 0;
    private static final int V4A_FX_TYPE_HEADPHONE = 1;
    private static final int V4A_FX_TYPE_SPEAKER = 2;

    /* ViPER4Android General FX Parameters */
    private static final int PARAM_FX_TYPE_SWITCH = 65537;
    private static final int PARAM_HPFX_CONV_PROCESS_ENABLED = 65538;
    private static final int PARAM_HPFX_CONV_PREPAREBUFFER = 65540;
    private static final int PARAM_HPFX_CONV_SETBUFFER = 65541;
    private static final int PARAM_HPFX_CONV_COMMITBUFFER = 65542;
    private static final int PARAM_HPFX_CONV_CROSSCHANNEL = 65543;
    private static final int PARAM_HPFX_VHE_PROCESS_ENABLED = 65544;
    private static final int PARAM_HPFX_VHE_EFFECT_LEVEL = 65545;
    private static final int PARAM_HPFX_FIREQ_PROCESS_ENABLED = 65546;
    private static final int PARAM_HPFX_FIREQ_BANDLEVEL = 65547;
    private static final int PARAM_HPFX_COLM_PROCESS_ENABLED = 65548;
    private static final int PARAM_HPFX_COLM_WIDENING = 65549;
    private static final int PARAM_HPFX_COLM_MIDIMAGE = 65550;
    private static final int PARAM_HPFX_COLM_DEPTH = 65551;
    private static final int PARAM_HPFX_DIFFSURR_PROCESS_ENABLED = 65552;
    private static final int PARAM_HPFX_DIFFSURR_DELAYTIME = 65553;
    private static final int PARAM_HPFX_REVB_PROCESS_ENABLED = 65554;
    private static final int PARAM_HPFX_REVB_ROOMSIZE = 65555;
    private static final int PARAM_HPFX_REVB_WIDTH = 65556;
    private static final int PARAM_HPFX_REVB_DAMP = 65557;
    private static final int PARAM_HPFX_REVB_WET = 65558;
    private static final int PARAM_HPFX_REVB_DRY = 65559;
    private static final int PARAM_HPFX_AGC_PROCESS_ENABLED = 65560;
    private static final int PARAM_HPFX_AGC_RATIO = 65561;
    private static final int PARAM_HPFX_AGC_VOLUME = 65562;
    private static final int PARAM_HPFX_AGC_MAXSCALER = 65563;
    private static final int PARAM_HPFX_DYNSYS_PROCESS_ENABLED = 65564;
    private static final int PARAM_HPFX_DYNSYS_XCOEFFS = 65565;
    private static final int PARAM_HPFX_DYNSYS_YCOEFFS = 65566;
    private static final int PARAM_HPFX_DYNSYS_SIDEGAIN = 65567;
    private static final int PARAM_HPFX_DYNSYS_BASSGAIN = 65568;
    private static final int PARAM_HPFX_VIPERBASS_PROCESS_ENABLED = 65569;
    private static final int PARAM_HPFX_VIPERBASS_MODE = 65570;
    private static final int PARAM_HPFX_VIPERBASS_SPEAKER = 65571;
    private static final int PARAM_HPFX_VIPERBASS_BASSGAIN = 65572;
    private static final int PARAM_HPFX_VIPERCLARITY_PROCESS_ENABLED = 65573;
    private static final int PARAM_HPFX_VIPERCLARITY_MODE = 65574;
    private static final int PARAM_HPFX_VIPERCLARITY_CLARITY = 65575;
    private static final int PARAM_HPFX_CURE_PROCESS_ENABLED = 65576;
    private static final int PARAM_HPFX_CURE_CROSSFEED = 65577;
    private static final int PARAM_HPFX_TUBE_PROCESS_ENABLED = 65578;
    private static final int PARAM_HPFX_OUTPUT_VOLUME = 65579;
    private static final int PARAM_HPFX_OUTPUT_PAN = 65580;
    private static final int PARAM_HPFX_LIMITER_THRESHOLD = 65581;
    private static final int PARAM_SPKFX_CONV_PROCESS_ENABLED = 65582;
    private static final int PARAM_SPKFX_CONV_PREPAREBUFFER = 65584;
    private static final int PARAM_SPKFX_CONV_SETBUFFER = 65585;
    private static final int PARAM_SPKFX_CONV_COMMITBUFFER = 65586;
    private static final int PARAM_SPKFX_CONV_CROSSCHANNEL = 65587;
    private static final int PARAM_SPKFX_FIREQ_PROCESS_ENABLED = 65588;
    private static final int PARAM_SPKFX_FIREQ_BANDLEVEL = 65589;
    private static final int PARAM_SPKFX_REVB_PROCESS_ENABLED = 65590;
    private static final int PARAM_SPKFX_REVB_ROOMSIZE = 65591;
    private static final int PARAM_SPKFX_REVB_WIDTH = 65592;
    private static final int PARAM_SPKFX_REVB_DAMP = 65593;
    private static final int PARAM_SPKFX_REVB_WET = 65594;
    private static final int PARAM_SPKFX_REVB_DRY = 65595;
    private static final int PARAM_SPKFX_CORR_PROCESS_ENABLED = 65596;
    private static final int PARAM_SPKFX_AGC_PROCESS_ENABLED = 65597;
    private static final int PARAM_SPKFX_AGC_RATIO = 65598;
    private static final int PARAM_SPKFX_AGC_VOLUME = 65599;
    private static final int PARAM_SPKFX_AGC_MAXSCALER = 65600;
    private static final int PARAM_SPKFX_OUTPUT_VOLUME = 65601;
    private static final int PARAM_SPKFX_LIMITER_THRESHOLD = 65602;

    private final LocalBinder mBinder = new LocalBinder();

    protected static boolean mUseHeadset = false;
    protected static boolean mUseBluetooth = false;
    protected static boolean mUseUSBSoundCard = false;
    protected static String mPreviousMode = "none";

    private float[] mOverriddenEqualizerLevels = null;
    private static boolean mDriverIsReady = false;
    private V4ADSPModule mGeneralFX = null;
    protected final Map<Integer, V4ADSPModule> mGeneralFXList = new HashMap<Integer, V4ADSPModule>();
    private ResourceMutex mV4AMutex = new ResourceMutex();

    private static final String ACTION_QUERY_DRIVERSTATUS = "com.vipercn.viper4android_v2.QUERY_DRIVERSTATUS";
    private static final String ACTION_QUERY_DRIVERSTATUS_RESULT = "com.vipercn.viper4android_v2.QUERY_DRIVERSTATUS_RESULT";
    private static final String ACTION_QUERY_EQUALIZER = "com.vipercn.viper4android_v2.QUERY_EQUALIZER";
    private static final String ACTION_QUERY_EQUALIZER_RESULT = "com.vipercn.viper4android_v2.QUERY_EQUALIZER_RESULT";
    private static final String ACTION_TAKEOVER_EFFECT = "com.vipercn.viper4android_v2.TAKEOVER_EFFECT";
    private static final String ACTION_TAKEOVER_EFFECT_RESULT = "com.vipercn.viper4android_v2.TAKEOVER_EFFECT_RESULT";
    private static final String ACTION_RELEASE_EFFECT = "com.vipercn.viper4android_v2.RELEASE_EFFECT";
    private static final String ACTION_SET_ENABLED = "com.vipercn.viper4android_v2.SET_ENABLED";
    private static final String ACTION_SET_EQUALIZER = "com.vipercn.viper4android_v2.SET_EQUALIZER";
    private boolean m3rdEnabled = false;
    private boolean m3rdEqualizerEnabled = false;
    private float[] m3rdEqualizerLevels = null;
    private boolean mWorkingWith3rd = false;

    /**
     * *** 3rd API Interface *****
     */
    private final BroadcastReceiver m3rdAPI_QUERY_DRIVERSTATUS_Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "m3rdAPI_QUERY_DRIVERSTATUS_Receiver::onReceive()");
            Intent itResult = new Intent(ACTION_QUERY_DRIVERSTATUS_RESULT);
            itResult.putExtra("driver_ready", mDriverIsReady);
            itResult.putExtra("enabled", getDriverEnabled());
            sendBroadcast(itResult);
        }
    };
    private final BroadcastReceiver m3rdAPI_QUERY_EQUALIZER_Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "m3rdAPI_QUERY_EQUALIZER_Receiver::onReceive()");
            Intent itResult = new Intent(ACTION_QUERY_EQUALIZER_RESULT);

            String mode = getAudioOutputRouting(getSharedPreferences(
                    ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE));
            SharedPreferences preferences = getSharedPreferences(
                    ViPER4Android.SHARED_PREFERENCES_BASENAME + "." + mode, 0);
            boolean mEqEnabled = preferences.getBoolean(
                    "viper4android.headphonefx.fireq.enable", false);
            itResult.putExtra("equalizer_enabled", mEqEnabled);
            itResult.putExtra("equalizer_bandcount", 10);
            float[] mEqBands = {
                    31.0f, 62.0f, 125.0f, 250.0f, 500.0f, 1000.0f, 2000.0f, 4000.0f, 8000.0f,
                    16000.0f
            };
            itResult.putExtra("equalizer_bandfreq", mEqBands);
            sendBroadcast(itResult);
        }
    };
    private final BroadcastReceiver m3rdAPI_TAKEOVER_EFFECT_Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "m3rdAPI_TAKEOVER_EFFECT_Receiver::onReceive()");
            Intent itResult = new Intent(ACTION_TAKEOVER_EFFECT_RESULT);

            if (!intent.hasExtra("token")) {
                Log.i("ViPER4Android", "m3rdAPI_TAKEOVER_EFFECT_Receiver, no token found");
                itResult.putExtra("granted", false);
                sendBroadcast(itResult);
            } else {
                int nToken = intent.getIntExtra("token", 0);
                if (nToken == 0) {
                    Log.i(TAG, "m3rdAPI_TAKEOVER_EFFECT_Receiver, invalid token found");
                    itResult.putExtra("granted", false);
                    sendBroadcast(itResult);
                } else {
                    mWorkingWith3rd = true;
                    Log.i("ViPER4Android", "m3rdAPI_TAKEOVER_EFFECT_Receiver, token = " + nToken);
                    itResult.putExtra("granted", true);
                    sendBroadcast(itResult);
                }
            }
        }
    };
    private final BroadcastReceiver m3rdAPI_RELEASE_EFFECT_Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "m3rdAPI_RELEASE_EFFECT_Receiver::onReceive()");
            mWorkingWith3rd = false;

            if (!intent.hasExtra("token")) {
                updateSystem(false);
            } else {
                int nToken = intent.getIntExtra("token", 0);
                Log.i(TAG, "m3rdAPI_RELEASE_EFFECT_Receiver, token = " + nToken);
                updateSystem(false);
            }
        }
    };
    private final BroadcastReceiver m3rdAPI_SET_ENABLED_Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "m3rdAPI_SET_ENABLED_Receiver::onReceive()");

            if (!mWorkingWith3rd)
                return;
            if (!intent.hasExtra("token")) {
                Log.i(TAG, "m3rdAPI_SET_ENABLED_Receiver, no token found");
            } else {
                int nToken = intent.getIntExtra("token", 0);
                if (nToken == 0) {
                    Log.i(TAG, "m3rdAPI_SET_ENABLED_Receiver, invalid token found");
                } else {
                    if (!intent.hasExtra("enabled"))
                        return;
                    m3rdEnabled = intent.getBooleanExtra("enabled", false);
                    Log.i(TAG, "m3rdAPI_SET_ENABLED_Receiver, token = "
                            + nToken + ", enabled = " + m3rdEnabled);
                    updateSystem(false);
                }
            }
        }
    };
    private final BroadcastReceiver m3rdAPI_SET_EQUALIZER_Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "m3rdAPI_SET_EQUALIZER_Receiver::onReceive()");

            if (!mWorkingWith3rd)
                return;
            if (!intent.hasExtra("token")) {
                Log.i(TAG, "m3rdAPI_SET_EQUALIZER_Receiver, no token found");
            } else {
                int nToken = intent.getIntExtra("token", 0);
                if (nToken == 0) {
                    Log.i(TAG, "m3rdAPI_SET_EQUALIZER_Receiver, invalid token found");
                    return;
                } else {
                    Log.i(TAG, "m3rdAPI_SET_EQUALIZER_Receiver, token = " + nToken);
                    if (intent.hasExtra("enabled")) {
                        m3rdEqualizerEnabled = intent.getBooleanExtra("enabled",
                                m3rdEqualizerEnabled);
                        Log.i(TAG, "m3rdAPI_SET_EQUALIZER_Receiver, enable equalizer = "
                                + m3rdEqualizerEnabled);
                    }
                    if (intent.hasExtra("bandcount") && intent.hasExtra("bandvalues")) {
                        int nBandCount = intent.getIntExtra("bandcount", 0);
                        float[] nBandValues = intent.getFloatArrayExtra("bandvalues");
                        if (nBandCount != 10 || nBandValues == null) {
                            Log.i("ViPER4Android", "m3rdAPI_SET_EQUALIZER_Receiver,"
                                    + "invalid band parameters");
                            return;
                        }
                        Log.i("ViPER4Android",
                                "m3rdAPI_SET_EQUALIZER_Receiver, got new eq band values");
                        if (m3rdEqualizerLevels == null)
                            m3rdEqualizerLevels = new float[10];
                        System.arraycopy(nBandValues, 0, m3rdEqualizerLevels, 0, nBandCount);
                    }
                }
                updateSystem(false);
            }
        }
    };
    /* *************************** */

    private final BroadcastReceiver mAudioSessionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "mAudioSessionReceiver::onReceive()");

            SharedPreferences prefSettings = getSharedPreferences(
                    ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
            String mCompatibleMode = prefSettings.getString(
                    "viper4android.settings.compatiblemode", "global");
            boolean mFXInLocalMode = !mCompatibleMode.equals("global");

            String action = intent.getAction();
            int sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0);
            if (sessionId == 0) {
                Log.i(TAG, "Global output mixer session control received! ");
                return;
            }

            if (action.equals(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)) {
                Log.i("ViPER4Android", String.format("New audio session: %d", sessionId));
                if (!mFXInLocalMode) {
                    Log.i("ViPER4Android", "Only global effect allowed.");
                    return;
                }
                if (mV4AMutex.acquire()) {
                    if (mGeneralFXList.containsKey(sessionId)) {
                        Log.i("ViPER4Android", "Creating local V4ADSPModule ...");
                        V4ADSPModule v4aNewDSPModule = new V4ADSPModule(sessionId);
                        if (v4aNewDSPModule.mInstance == null) {
                            Log.e("ViPER4Android", "Failed to load v4a driver.");
                            v4aNewDSPModule.release();
                        } else
                            mGeneralFXList.put(sessionId, v4aNewDSPModule);
                    } else {
                        Log.i("ViPER4Android", "Effect module already exist [session = "
                                + sessionId + "]");
                        mV4AMutex.release();
                        return;
                    }
                    mV4AMutex.release();
                } else
                    Log.i("ViPER4Android", "Semaphore acquire failed.");
            }

            if (action.equals(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)) {
                Log.i("ViPER4Android", String.format("Audio session removed: %d", sessionId));
                if (mV4AMutex.acquire()) {
                    if (mGeneralFXList.containsKey(sessionId)) {
                        V4ADSPModule v4aRemove = mGeneralFXList.get(sessionId);
                        mGeneralFXList.remove(sessionId);
                        if (v4aRemove != null)
                            v4aRemove.release();
                    }
                    mV4AMutex.release();
                } else
                    Log.i("ViPER4Android", "Semaphore accquire failed.");
            }

            updateSystem(false);
        }
    };

    private final BroadcastReceiver mPreferenceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "mPreferenceUpdateReceiver::onReceive()");
            updateSystem(false);
        }
    };

    private final BroadcastReceiver mShowNotifyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "mShowNotifyReceiver::onReceive()");

            String mode = getAudioOutputRouting(getSharedPreferences(
                    ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE));
            if (mode.equalsIgnoreCase("headset")) {
                showNotification(getString(getResources().getIdentifier("text_headset", "string",
                        getApplicationInfo().packageName)));
            } else if (mode.equalsIgnoreCase("bluetooth")) {
                showNotification(getString(getResources().getIdentifier("text_bluetooth", "string",
                        getApplicationInfo().packageName)));
            } else if (mode.equalsIgnoreCase("usb")) {
                showNotification(getString(getResources().getIdentifier("text_usb", "string",
                        getApplicationInfo().packageName)));
            } else {
                showNotification(getString(getResources().getIdentifier("text_speaker", "string",
                        getApplicationInfo().packageName)));
            }
        }
    };

    private final BroadcastReceiver mCancelNotifyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("ViPER4Android", "mCancelNotifyReceiver::onReceive()");
            cancelNotification();
        }
    };

    private final BroadcastReceiver mRoutingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.i("ViPER4Android", "mRoutingReceiver::onReceive()");

            final String action = intent.getAction();
            final boolean prevUseHeadset = mUseHeadset;
            final boolean prevUseBluetooth = mUseBluetooth;
            final boolean prevUseUSB = mUseUSBSoundCard;

            if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                mUseHeadset = intent.getIntExtra("state", 0) == 1;
            } else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE,
                        BluetoothA2dp.STATE_DISCONNECTED);
                mUseBluetooth = state == BluetoothA2dp.STATE_CONNECTED;
            } else {
                if (Build.VERSION.SDK_INT >= 16 && action.equals(
                        "android.intent.action.ANALOG_AUDIO_DOCK_PLUG")) {
                    mUseUSBSoundCard = intent.getIntExtra("state", 0) == 1;
                }
            }

            Log.i("ViPER4Android", "Headset=" + mUseHeadset + ", Bluetooth=" + mUseBluetooth
                    + ", USB=" + mUseUSBSoundCard);
            if (prevUseHeadset != mUseHeadset
                    || prevUseBluetooth != mUseBluetooth
                    || prevUseUSB != mUseUSBSoundCard) {
                /* Audio output method changed, so we flush buffer */
                updateSystem(true);
            }
        }
    };

    private void showNotification(String mFXType) {
        SharedPreferences preferences = getSharedPreferences(
                ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
        boolean enableNotify = preferences.getBoolean(
                "viper4android.settings.show_notify_icon", false);
        if (!enableNotify) {
            Log.i("ViPER4Android", "showNotification(): show_notify = false");
            return;
        }

        int mIconID = getResources().getIdentifier("icon", "drawable",
                getApplicationInfo().packageName);
        String mNotifyText = "ViPER4Android FX " + mFXType;
        CharSequence contentTitle = "ViPER4Android FX";
        Intent notificationIntent = new Intent(this, ViPER4Android.class);
        PendingIntent contentItent = PendingIntent.getActivity(
                this, 0, notificationIntent, 0);

        if (contentItent != null) {
            Notification v4aNotify = new Notification.Builder(this)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setDefaults(0)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(mIconID)
                    .setTicker(mNotifyText)
                    .setContentTitle(contentTitle)
                    .setContentText(mFXType)
                    .setContentIntent(contentItent)
                    .build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    android.content.Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(0x1234, v4aNotify);
            }
        }
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(0x1234);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("ViPER4Android", "Query ViPER4Android engine ...");
        Utils.AudioEffectUtils aeuUtils = new Utils().new AudioEffectUtils();
        if (!aeuUtils.isViPER4AndroidEngineFound()) {
            Log.i("ViPER4Android", "ViPER4Android engine not found, create empty service");
            mDriverIsReady = false;
            return;
        } else {
            PackageManager packageMgr = getPackageManager();
            PackageInfo packageInfo;
            String apkVersion;
            try {
                int[] iaDrvVer = aeuUtils.getViper4AndroidEngineVersion();
                String mDriverVersion = iaDrvVer[0] + "." + iaDrvVer[1] + "." + iaDrvVer[2] + "."
                        + iaDrvVer[3];
                packageInfo = packageMgr.getPackageInfo(getPackageName(), 0);
                apkVersion = packageInfo.versionName;
                if (!apkVersion.equalsIgnoreCase(mDriverVersion)) {
                    Log.i("ViPER4Android", "ViPER4Android engine is not compatible with service");
                    mDriverIsReady = false;
                    return;
                }
            } catch (NameNotFoundException e) {
                Log.i("ViPER4Android", "Cannot find ViPER4Android's apk [weird]");
                mDriverIsReady = false;
                return;
            }
        }
        mDriverIsReady = true;

        Context context = getApplicationContext();
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mUseBluetooth = mAudioManager.isBluetoothA2dpOn();
            if (mUseBluetooth) {
                Log.i("ViPER4Android", "Current is a2dp mode [bluetooth]");
                mUseHeadset = false;
                mUseUSBSoundCard = false;
            } else {
                mUseHeadset = mAudioManager.isWiredHeadsetOn();
                if (mUseHeadset) {
                    Log.i("ViPER4Android", "Current is headset mode");
                    mUseUSBSoundCard = false;
                } else {
                    Log.i("ViPER4Android", "Current is speaker mode");
                    mUseUSBSoundCard = false;
                }
            }
        }
        Log.i("ViPER4Android", "Get current mode from system [" + getAudioOutputRouting(
                getSharedPreferences(ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings",
                        MODE_PRIVATE)) + "]");

        if (mGeneralFX != null) {
            Log.e("ViPER4Android", "onCreate, mGeneralFX != null");
            mGeneralFX.release();
            mGeneralFX = null;
        }

        SharedPreferences prefSettings = getSharedPreferences(
                ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", 0);
        boolean mDriverConfigured = prefSettings.getBoolean(
                "viper4android.settings.driverconfigured", false);
        if (!mDriverConfigured) {
            Editor editPrefs = prefSettings.edit();
            if (editPrefs != null) {
                editPrefs.putBoolean("viper4android.settings.driverconfigured", true);
                editPrefs.commit();
            }
        }
        String mCompatibleMode = prefSettings.getString(
                "viper4android.settings.compatiblemode", "global");
        if (mCompatibleMode.equalsIgnoreCase("global")) {
            Log.i("ViPER4Android", "Creating global V4ADSPModule ...");
            if (mGeneralFX == null)
                mGeneralFX = new V4ADSPModule(DEVICE_GLOBAL_OUTPUT_MIXER);
            if (mGeneralFX.mInstance == null) {
                Log.e("ViPER4Android", "Found v4a driver, but failed to load.");
                mGeneralFX.release();
                mGeneralFX = null;
            }
        }

        if (Build.VERSION.SDK_INT < 18)
            startForeground(ViPER4Android.NOTIFY_FOREGROUND_ID, new Notification());

        IntentFilter audioSessionFilter = new IntentFilter();
        audioSessionFilter.addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        audioSessionFilter.addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        registerReceiver(mAudioSessionReceiver, audioSessionFilter);

        final IntentFilter audioFilter = new IntentFilter();
        audioFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        if (Build.VERSION.SDK_INT >= 16) {
            // Equals Intent.ACTION_ANALOG_AUDIO_DOCK_PLUG
            audioFilter.addAction("android.intent.action.ANALOG_AUDIO_DOCK_PLUG");
        }
        audioFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        audioFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mRoutingReceiver, audioFilter);

        registerReceiver(mPreferenceUpdateReceiver,
                new IntentFilter(ViPER4Android.ACTION_UPDATE_PREFERENCES));
        registerReceiver(mShowNotifyReceiver, new IntentFilter(ViPER4Android.ACTION_SHOW_NOTIFY));
        registerReceiver(mCancelNotifyReceiver,
                new IntentFilter(ViPER4Android.ACTION_CANCEL_NOTIFY));

        /******************** 3rd API Interface ********************/
        registerReceiver(m3rdAPI_QUERY_DRIVERSTATUS_Receiver,
                new IntentFilter(ACTION_QUERY_DRIVERSTATUS));
        registerReceiver(m3rdAPI_QUERY_EQUALIZER_Receiver,
                new IntentFilter(ACTION_QUERY_EQUALIZER));
        registerReceiver(m3rdAPI_TAKEOVER_EFFECT_Receiver,
                new IntentFilter(ACTION_TAKEOVER_EFFECT));
        registerReceiver(m3rdAPI_RELEASE_EFFECT_Receiver, new IntentFilter(ACTION_RELEASE_EFFECT));
        registerReceiver(m3rdAPI_SET_ENABLED_Receiver, new IntentFilter(ACTION_SET_ENABLED));
        registerReceiver(m3rdAPI_SET_EQUALIZER_Receiver, new IntentFilter(ACTION_SET_EQUALIZER));
        /***********************************************************/

        Log.i("ViPER4Android", "Service launched.");

        updateSystem(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (!mDriverIsReady)
            return;

        if (Build.VERSION.SDK_INT < 18) {
            stopForeground(true);
        }

        unregisterReceiver(mAudioSessionReceiver);
        unregisterReceiver(mRoutingReceiver);
        unregisterReceiver(mPreferenceUpdateReceiver);
        unregisterReceiver(mShowNotifyReceiver);
        unregisterReceiver(mCancelNotifyReceiver);

        /******************** 3rd API Interface ********************/
        unregisterReceiver(m3rdAPI_QUERY_DRIVERSTATUS_Receiver);
        unregisterReceiver(m3rdAPI_QUERY_EQUALIZER_Receiver);
        unregisterReceiver(m3rdAPI_TAKEOVER_EFFECT_Receiver);
        unregisterReceiver(m3rdAPI_RELEASE_EFFECT_Receiver);
        unregisterReceiver(m3rdAPI_SET_ENABLED_Receiver);
        unregisterReceiver(m3rdAPI_SET_EQUALIZER_Receiver);
        /***********************************************************/

        cancelNotification();

        if (mGeneralFX != null)
            mGeneralFX.release();
        mGeneralFX = null;

        if (mV4AMutex.acquire()) {
            for (Integer sessionId : new ArrayList<Integer>(mGeneralFXList.keySet())) {
                V4ADSPModule v4aModule = mGeneralFXList.get(sessionId);
                if ((sessionId < 0) || (v4aModule == null))
                    continue;
                v4aModule.release();
            }
            mGeneralFXList.clear();
            mV4AMutex.release();
        }

        Log.i("ViPER4Android", "Service destroyed.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We should do some driver check in this method, if the driver is
        // abnormal, we need to reload it

        Log.i("ViPER4Android", "Service::onStartCommand [Begin check driver]");

        if (!mDriverIsReady) {
            Log.e("ViPER4Android", "Service::onStartCommand [V4A Engine not found]");
            return super.onStartCommand(intent, flags, startId);
        }

        SharedPreferences prefSettings = getSharedPreferences(
                ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", 0);
        String mCompatibleMode = prefSettings
                .getString("viper4android.settings.compatiblemode", "global");
        if (!mCompatibleMode.equalsIgnoreCase("global")) {
            Log.i("ViPER4Android", "Service::onStartCommand [V4A is local effect mode]");
            return super.onStartCommand(intent, flags, startId);
        }

        if (mGeneralFX == null) {
            // Create engine instance
            Log.i(TAG, "Service::onStartCommand [Creating global V4ADSPModule ...]");
            mGeneralFX = new V4ADSPModule(DEVICE_GLOBAL_OUTPUT_MIXER);
            if (mGeneralFX.mInstance == null) {
                // If we reach here, it means android refuse to load v4a driver.
                // There are only two cases:
                // 1. The android system not totally booted or media server
                // crashed.
                // 2. The v4a driver installed not compitable with this android.
                Log.e("ViPER4Android",
                        "Service::onStartCommand [Found v4a driver, but failed to load]");
                mGeneralFX.release();
                mGeneralFX = null;
                return super.onStartCommand(intent, flags, startId);
            }

            // Engine instance created, update parameters
            Log.i("ViPER4Android", "Service::onStartCommand [V4ADSPModule created]");
            updateSystem(true); // After all parameters commited, please reset
                                // all effects
            return super.onStartCommand(intent, flags, startId);
        }

        if (mGeneralFX.mInstance == null) {
            // We shouldn't go here, but ...
            // Recreate engine instance
            mGeneralFX.release();
            Log.i(TAG, "Service::onStartCommand [Recreating global V4ADSPModule ...]");
            mGeneralFX = new V4ADSPModule(DEVICE_GLOBAL_OUTPUT_MIXER);
            if (mGeneralFX.mInstance == null) {
                // If we reach here, it means android refuse to load v4a driver.
                // There are only two cases:
                // 1. The android system not totally booted or media server
                // crashed.
                // 2. The v4a driver installed not compatible with this android.
                Log.e("ViPER4Android",
                        "Service::onStartCommand [Found v4a driver, but failed to load]");
                mGeneralFX.release();
                mGeneralFX = null;
                return super.onStartCommand(intent, flags, startId);
            }

            // Engine instance created, update parameters
            Log.i("ViPER4Android", "Service::onStartCommand [V4ADSPModule created]");
            updateSystem(true); // After all parameters commited, please reset
                                // all effects
            return super.onStartCommand(intent, flags, startId);
        }

        if (!getDriverCanWork()) {
            // V4A driver is malfunction, but what caused this?
            // 1. Low ram available.
            // 2. Android audio hal bug.
            // 3. Media server crashed.

            // Since v2.3.2.5 we have driver self diagnose feature
            // So we request a self diagnose procedure here
            Log.i("ViPER4Android", "Service::onStartCommand [Begin self diagnose procedure ...]");
            mGeneralFX.setParameter(PARAM_SET_SELFDIAGNOSE_STATUS, 0);
            if (!getDriverCanWork()) {
                // If we still got problem, it means the effect factory
                // corrupted, the only way is reload the driver
                mGeneralFX.release();
                Log.i("ViPER4Android",
                        "Service::onStartCommand [Recreating global V4ADSPModule ...]");
                mGeneralFX = new V4ADSPModule(DEVICE_GLOBAL_OUTPUT_MIXER);
                if (mGeneralFX.mInstance == null) {
                    // If we reach here, it means android refuse to load v4a
                    // driver.
                    // There are only two cases:
                    // 1. The android system not totally booted or media server
                    // crashed.
                    // 2. The v4a driver installed not compatible with this
                    // android.
                    Log.e("ViPER4Android",
                            "Service::onStartCommand [Found v4a driver, but failed to load]");
                    mGeneralFX.release();
                    mGeneralFX = null;
                    return super.onStartCommand(intent, flags, startId);
                } else {
                    // Engine fixed
                    Log.i("ViPER4Android", "Service::onStartCommand [Engine fixed]");
                    updateSystem(true); // Please reset effects
                }
            } else {
                // Engine fixed
                Log.i("ViPER4Android", "Service::onStartCommand [Engine fixed]");
                updateSystem(true); // Please reset effects
            }

            return super.onStartCommand(intent, flags, startId);
        }

        Log.i("ViPER4Android", "Service::onStartCommand [Everything is ok]");

        return super.onStartCommand(intent, flags, startId);
    }

    public void setEqualizerLevels(float[] levels) {
        mOverriddenEqualizerLevels = levels;
        updateSystem(false);
    }

    public static String getAudioOutputRouting(SharedPreferences prefSettings) {
        String mLockedEffect = prefSettings.getString(
                "viper4android.settings.lock_effect", "none");
        if (mLockedEffect.equalsIgnoreCase("none")) {
            if (mUseBluetooth)
                return "bluetooth";
            if (mUseHeadset)
                return "headset";
            if (mUseUSBSoundCard)
                return "usb";
            return "speaker";
        }
        return mLockedEffect;
    }

    public boolean getDriverIsReady() {
        return mDriverIsReady;
    }

    public boolean getDriverLoaded() {
        return !(!mDriverIsReady || mGeneralFX == null || mGeneralFX.mInstance == null);
    }

    public void startStatusUpdating() {
        if (mGeneralFX != null && mDriverIsReady)
            mGeneralFX.setParameter(PARAM_SET_UPDATE_STATUS, 1);
    }

    public void stopStatusUpdating() {
        if (mGeneralFX != null && mDriverIsReady)
            mGeneralFX.setParameter(PARAM_SET_UPDATE_STATUS, 0);
    }

    public boolean getDriverNEON() {
        boolean mResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_NEONENABLED) == 1)
                mResult = true;
        }
        return mResult;
    }

    public boolean getDriverEnabled() {
        boolean mResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_ENABLED) == 1)
                mResult = true;
        }
        return mResult;
    }

    public boolean getDriverSupportFormat() {
        boolean mResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_CONFIGURE) == 1)
                mResult = true;
        }
        return mResult;
    }

    public boolean getDriverCanWork() {
        boolean mResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_DRVCANWORK) == 1)
                mResult = true;
        }
        return mResult;
    }

    public boolean getDriverProcess() {
        boolean mResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_STREAMING) == 1)
                mResult = true;
        }
        return mResult;
    }

    public int getDriverSamplingRate() {
        int nResult = 0;
        if (mGeneralFX != null && mDriverIsReady)
            nResult = mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_SAMPLINGRATE);
        return nResult;
    }

    public boolean getConvolverUsable() {
        boolean mResult = false;
        if (mGeneralFX != null && mDriverIsReady) {
            if (mGeneralFX.getParameter_px4_vx4x1(PARAM_GET_CONVUSABLE) == 1)
                mResult = true;
        }
        return mResult;
    }

    void setV4AEqualizerBandLevel(int idx, int level, boolean hpfx, V4ADSPModule dsp) {
        if (dsp == null || !mDriverIsReady)
            return;
        if (hpfx) {
            dsp.setParameter(PARAM_HPFX_FIREQ_BANDLEVEL, idx, level);
        } else {
            dsp.setParameter(PARAM_SPKFX_FIREQ_BANDLEVEL, idx, level);
        }
    }

    void updateSystem(boolean mRequireReset) {
        String mode = getAudioOutputRouting(getSharedPreferences(
                ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE));
        SharedPreferences preferences = getSharedPreferences(
                ViPER4Android.SHARED_PREFERENCES_BASENAME + "." + mode, 0);
        Log.i("ViPER4Android", "Begin system update(" + mode + ")");

        int mFXType = V4A_FX_TYPE_HEADPHONE;
        if (mode.equalsIgnoreCase("speaker"))
            mFXType = V4A_FX_TYPE_SPEAKER;

        if (!mode.equalsIgnoreCase(mPreviousMode)) {
            mPreviousMode = mode;
            if (mode.equalsIgnoreCase("headset"))
                showNotification(getString(getResources().getIdentifier("text_headset", "string",
                        getApplicationInfo().packageName)));
            else if (mode.equalsIgnoreCase("bluetooth"))
                showNotification(getString(getResources().getIdentifier("text_bluetooth", "string",
                        getApplicationInfo().packageName)));
            else if (mode.equalsIgnoreCase("usb"))
                showNotification(getString(getResources().getIdentifier("text_usb", "string",
                        getApplicationInfo().packageName)));
            else
                showNotification(getString(getResources().getIdentifier("text_speaker", "string",
                        getApplicationInfo().packageName)));
        }

        SharedPreferences prefSettings = getSharedPreferences(
                ViPER4Android.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
        String mCompatibleMode = prefSettings
                .getString("viper4android.settings.compatiblemode", "global");
        boolean mFXInLocalMode;
        mFXInLocalMode = !mCompatibleMode.equals("global");

        Log.i("ViPER4Android", "<+++++++++++++++ Update global effect +++++++++++++++>");
        updateSystem_Global(preferences, mFXType, mRequireReset, mFXInLocalMode);
        Log.i("ViPER4Android", "<++++++++++++++++++++++++++++++++++++++++++++++++++++>");

        Log.i("ViPER4Android", "<+++++++++++++++ Update local effect +++++++++++++++>");
        updateSystem_Local(preferences, mFXType, mRequireReset, mFXInLocalMode);
        Log.i("ViPER4Android", "<+++++++++++++++++++++++++++++++++++++++++++++++++++>");
    }

    void updateSystem_Global(SharedPreferences preferences, int mFXType, boolean mRequireReset,
            boolean mLocalFX) {
        if (mGeneralFX == null || mGeneralFX.mInstance == null || !mDriverIsReady) {
            Log.i("ViPER4Android", "updateSystem(): Effects is invalid!");
            return;
        }

        try {
            if (!mGeneralFX.mInstance.hasControl()) {
                Log.i("ViPER4Android", "The effect is controlling by system now");
                return;
            }
        } catch (Exception e) {
            Log.i("ViPER4Android", "updateSystem_Global(), Exception = " + e.getMessage());
            return;
        }

        if (mLocalFX)
            updateSystem_Module(preferences, mFXType, mGeneralFX, mRequireReset, true);
        else
            updateSystem_Module(preferences, mFXType, mGeneralFX, mRequireReset, false);
    }

    void updateSystem_Local(SharedPreferences preferences, int mFXType, boolean mRequireReset,
            boolean mLocalFX) {
        if (mV4AMutex.acquire()) {
            List<Integer> v4aUnderControl = new ArrayList<Integer>();
            for (Integer sessionId : new ArrayList<Integer>(mGeneralFXList.keySet())) {
                V4ADSPModule v4aModule = mGeneralFXList.get(sessionId);
                if ((sessionId < 0) || (v4aModule == null))
                    continue;
                try {
                    if (!mLocalFX)
                        updateSystem_Module(preferences, mFXType, v4aModule, mRequireReset, true);
                    else
                        updateSystem_Module(preferences, mFXType, v4aModule, mRequireReset, false);
                } catch (Exception e) {
                    Log.i("ViPER4Android",
                            String.format("Trouble trying to manage session %d, removing...",
                                    sessionId));
                    v4aUnderControl.add(sessionId);
                }
            }
            for (Integer aV4aUnderControl : v4aUnderControl)
                mGeneralFXList.remove(aV4aUnderControl);

            mV4AMutex.release();
        } else
            Log.i("ViPER4Android", "Semaphore accquire failed.");
    }

    void updateSystem_Module(SharedPreferences preferences, int mFXType, V4ADSPModule v4aModule,
            boolean requireReset, boolean mMasterSwitchOff) {
        Log.i("ViPER4Android", "updateSystem(): Commiting effects type");
        v4aModule.setParameter(PARAM_FX_TYPE_SWITCH, mFXType);

        /******************************************** Headphone FX ********************************************/
        if (mFXType == V4A_FX_TYPE_HEADPHONE) {
            Log.i("ViPER4Android", "updateSystem(): Commiting headphone-fx parameters");

            /* FIR Equalizer */
            Log.i("ViPER4Android", "updateSystem(): Updating FIR Equalizer.");
            if (!mWorkingWith3rd) {
                if (mOverriddenEqualizerLevels != null) {
                    for (int i = 0; i < mOverriddenEqualizerLevels.length; i++)
                        setV4AEqualizerBandLevel(i,
                                Math.round(mOverriddenEqualizerLevels[i] * 100),
                                true, v4aModule);
                } else {
                    String[] levels = preferences.getString(
                            "viper4android.headphonefx.fireq.custom",
                            "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;").split(";");
                    for (short i = 0; i < levels.length; i++) {
                        setV4AEqualizerBandLevel(i, Math.round(Float.valueOf(levels[i]) * 100),
                                true, v4aModule);
                    }
                }
                if (preferences.getBoolean("viper4android.headphonefx.fireq.enable", false))
                    v4aModule.setParameter(PARAM_HPFX_FIREQ_PROCESS_ENABLED, 1);
                else
                    v4aModule.setParameter(PARAM_HPFX_FIREQ_PROCESS_ENABLED, 0);
            } else {
                if (m3rdEqualizerLevels != null) {
                    for (int i = 0; i < m3rdEqualizerLevels.length; i++)
                        setV4AEqualizerBandLevel(i, Math.round(m3rdEqualizerLevels[i] * 100),
                                true, v4aModule);
                }
                if (m3rdEqualizerEnabled)
                    v4aModule.setParameter(PARAM_HPFX_FIREQ_PROCESS_ENABLED, 1);
                else
                    v4aModule.setParameter(PARAM_HPFX_FIREQ_PROCESS_ENABLED, 0);
            }

            /* Convolver */
            Log.i("ViPER4Android", "updateSystem(): Updating Convolver.");
            String mConvIRFileName = preferences.getString(
                    "viper4android.headphonefx.convolver.kernel", "");
            v4aModule.setConvIRFile(mConvIRFileName, false);
            v4aModule
                    .setParameter(PARAM_HPFX_CONV_CROSSCHANNEL, Integer.valueOf(
                            preferences.getString(
                                    "viper4android.headphonefx.convolver.crosschannel", "0")));
            if (preferences.getBoolean("viper4android.headphonefx.convolver.enable", false))
                v4aModule.setParameter(PARAM_HPFX_CONV_PROCESS_ENABLED, 1);
            else
                v4aModule.setParameter(PARAM_HPFX_CONV_PROCESS_ENABLED, 0);

            /* Colorful Music (ViPER's Headphone 360) */
            Log.i("ViPER4Android", "updateSystem(): Updating Field Surround (Colorful Music).");
            String[] colorfulMusic = preferences.getString(
                    "viper4android.headphonefx.colorfulmusic.coeffs", "120;200")
                    .split(";");
            if (colorfulMusic.length == 2) {
                v4aModule.setParameter(PARAM_HPFX_COLM_WIDENING, Integer.valueOf(colorfulMusic[0]));
                v4aModule.setParameter(PARAM_HPFX_COLM_DEPTH, Integer.valueOf(colorfulMusic[1]));
            }
            v4aModule.setParameter(PARAM_HPFX_COLM_MIDIMAGE, Integer.valueOf(preferences
                    .getString("viper4android.headphonefx.colorfulmusic.midimage", "150")));
            if (preferences.getBoolean("viper4android.headphonefx.colorfulmusic.enable", false))
                v4aModule.setParameter(PARAM_HPFX_COLM_PROCESS_ENABLED, 1);
            else
                v4aModule.setParameter(PARAM_HPFX_COLM_PROCESS_ENABLED, 0);

            /* Diff Surround */
            Log.i("ViPER4Android", "updateSystem(): Updating Diff Surround.");
            v4aModule.setParameter(PARAM_HPFX_DIFFSURR_DELAYTIME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.diffsurr.delay", "500")));
            if (preferences.getBoolean("viper4android.headphonefx.diffsurr.enable", false))
                v4aModule.setParameter(PARAM_HPFX_DIFFSURR_PROCESS_ENABLED, 1);
            else
                v4aModule.setParameter(PARAM_HPFX_DIFFSURR_PROCESS_ENABLED, 0);

            /* ViPER's Headphone Surround Engine + */
            Log.i("ViPER4Android", "updateSystem(): Updating ViPER's Headphone Surround Engine +.");
            v4aModule.setParameter(PARAM_HPFX_VHE_EFFECT_LEVEL, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.vhs.qual", "0")));
            if (preferences.getBoolean("viper4android.headphonefx.vhs.enable", false))
                v4aModule.setParameter(PARAM_HPFX_VHE_PROCESS_ENABLED, 1);
            else
                v4aModule.setParameter(PARAM_HPFX_VHE_PROCESS_ENABLED, 0);

            /* ViPER's Reverberation */
            Log.i("ViPER4Android", "updateSystem(): Updating Reverberation.");
            v4aModule.setParameter(PARAM_HPFX_REVB_ROOMSIZE, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.roomsize", "0")));
            v4aModule.setParameter(PARAM_HPFX_REVB_WIDTH, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.roomwidth", "0")));
            v4aModule.setParameter(PARAM_HPFX_REVB_DAMP, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.damp", "0")));
            v4aModule.setParameter(PARAM_HPFX_REVB_WET, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.wet", "0")));
            v4aModule.setParameter(PARAM_HPFX_REVB_DRY, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.dry", "50")));
            if (preferences.getBoolean("viper4android.headphonefx.reverb.enable", false))
                v4aModule.setParameter(PARAM_HPFX_REVB_PROCESS_ENABLED, 1);
            else
                v4aModule.setParameter(PARAM_HPFX_REVB_PROCESS_ENABLED, 0);

            /* Playback Auto Gain Control */
            Log.i("ViPER4Android", "updateSystem(): Updating Playback AGC.");
            v4aModule.setParameter(PARAM_HPFX_AGC_RATIO, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.ratio", "50")));
            v4aModule.setParameter(PARAM_HPFX_AGC_VOLUME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.volume", "80")));
            v4aModule.setParameter(PARAM_HPFX_AGC_MAXSCALER, Integer.valueOf(preferences
                    .getString("viper4android.headphonefx.playbackgain.maxscaler", "400")));
            if (preferences.getBoolean("viper4android.headphonefx.playbackgain.enable", false)) {
                v4aModule.setParameter(PARAM_HPFX_AGC_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_HPFX_AGC_PROCESS_ENABLED, 0);
            }

            /* Dynamic System */
            Log.i("ViPER4Android", "updateSystem(): Updating Dynamic System.");
            String[] dsParameter = preferences.getString(
                    "viper4android.headphonefx.dynamicsystem.coeffs",
                    "100;5600;40;40;50;50").split(";");
            if (dsParameter.length == 6) {
                v4aModule.setParameter(PARAM_HPFX_DYNSYS_XCOEFFS,
                        Integer.valueOf(dsParameter[0]), Integer.valueOf(dsParameter[1]));
                v4aModule.setParameter(PARAM_HPFX_DYNSYS_YCOEFFS,
                        Integer.valueOf(dsParameter[2]), Integer.valueOf(dsParameter[3]));
                v4aModule.setParameter(PARAM_HPFX_DYNSYS_SIDEGAIN,
                        Integer.valueOf(dsParameter[4]), Integer.valueOf(dsParameter[5]));
            }
            int dsBass = Integer.valueOf(preferences.getString(
                    "viper4android.headphonefx.dynamicsystem.bass", "0"));
            dsBass = dsBass * 20 + 100;
            v4aModule.setParameter(PARAM_HPFX_DYNSYS_BASSGAIN, dsBass);
            if (preferences.getBoolean("viper4android.headphonefx.dynamicsystem.enable", false)) {
                v4aModule.setParameter(PARAM_HPFX_DYNSYS_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_HPFX_DYNSYS_PROCESS_ENABLED, 0);
            }

            /* Fidelity Control */
            Log.i("ViPER4Android", "updateSystem(): Updating Fidelity Control.");
            v4aModule.setParameter(PARAM_HPFX_VIPERBASS_MODE, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.fidelity.bass.mode", "0")));
            v4aModule.setParameter(PARAM_HPFX_VIPERBASS_SPEAKER, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.fidelity.bass.freq", "40")));
            v4aModule.setParameter(PARAM_HPFX_VIPERBASS_BASSGAIN, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.fidelity.bass.gain", "50")));
            if (preferences.getBoolean("viper4android.headphonefx.fidelity.bass.enable", false)) {
                v4aModule.setParameter(PARAM_HPFX_VIPERBASS_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_HPFX_VIPERBASS_PROCESS_ENABLED, 0);
            }
            v4aModule.setParameter(PARAM_HPFX_VIPERCLARITY_MODE, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.fidelity.clarity.mode", "0")));
            v4aModule.setParameter(PARAM_HPFX_VIPERCLARITY_CLARITY, Integer.valueOf(
                    preferences
                            .getString("viper4android.headphonefx.fidelity.clarity.gain", "50")));
            if (preferences
                    .getBoolean("viper4android.headphonefx.fidelity.clarity.enable", false)) {
                v4aModule.setParameter(PARAM_HPFX_VIPERCLARITY_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_HPFX_VIPERCLARITY_PROCESS_ENABLED, 0);
            }

            /* Cure System */
            Log.i("ViPER4Android", "updateSystem(): Updating Cure System.");
            v4aModule.setParameter(PARAM_HPFX_CURE_CROSSFEED, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.cure.crossfeed", "0")));
            if (preferences.getBoolean("viper4android.headphonefx.cure.enable", false)) {
                v4aModule.setParameter(PARAM_HPFX_CURE_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_HPFX_CURE_PROCESS_ENABLED, 0);
            }

            /* Tube Simulator */
            Log.i("ViPER4Android", "updateSystem(): Updating Tube Simulator.");
            if (preferences.getBoolean("viper4android.headphonefx.tube.enable", false)) {
                v4aModule.setParameter(PARAM_HPFX_TUBE_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_HPFX_TUBE_PROCESS_ENABLED, 0);
            }

            /* Speaker Optimization */
            Log.i("ViPER4Android", "updateSystem(): Shutting down speaker optimizer.");
            v4aModule.setParameter(PARAM_SPKFX_CORR_PROCESS_ENABLED, 0);

            /* Limiter */
            Log.i("ViPER4Android", "updateSystem(): Updating Limiter.");
            v4aModule.setParameter(PARAM_HPFX_OUTPUT_VOLUME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.outvol", "100")));
            v4aModule.setParameter(PARAM_HPFX_OUTPUT_PAN, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.channelpan", "0")));
            v4aModule.setParameter(PARAM_HPFX_LIMITER_THRESHOLD, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.limiter", "100")));

            /* Master Switch */
            if (!mWorkingWith3rd) {
                boolean forceEnable = preferences.getBoolean(
                        "viper4android.global.forceenable.enable", false);
                if (forceEnable) {
                    v4aModule.setParameter(PARAM_SET_FORCEENABLE_STATUS, 1);
                } else {
                    v4aModule.setParameter(PARAM_SET_FORCEENABLE_STATUS, 0);
                }

                boolean masterControl = preferences.getBoolean(
                        "viper4android.headphonefx.enable", false);
                if (mMasterSwitchOff) {
                    masterControl = false;
                }
                if (masterControl) {
                    v4aModule.setParameter(PARAM_SET_DOPROCESS_STATUS, 1);
                } else {
                    v4aModule.setParameter(PARAM_SET_DOPROCESS_STATUS, 0);
                }
                v4aModule.mInstance.setEnabled(masterControl);
            } else {
                if (m3rdEnabled) {
                    v4aModule.setParameter(PARAM_SET_DOPROCESS_STATUS, 1);
                    v4aModule.mInstance.setEnabled(true);
                } else {
                    v4aModule.setParameter(PARAM_SET_DOPROCESS_STATUS, 0);
                    v4aModule.mInstance.setEnabled(false);
                }
            }
        }
        /******************************************************************************************************/
        /********************************************* Speaker FX *********************************************/
        else if (mFXType == V4A_FX_TYPE_SPEAKER) {
            Log.i("ViPER4Android", "updateSystem(): Commiting speaker-fx parameters");

            /* FIR Equalizer */
            Log.i("ViPER4Android", "updateSystem(): Updating FIR Equalizer.");
            if (!mWorkingWith3rd) {
                if (mOverriddenEqualizerLevels != null) {
                    for (int i = 0; i < mOverriddenEqualizerLevels.length; i++) {
                        setV4AEqualizerBandLevel(i,
                                Math.round(mOverriddenEqualizerLevels[i] * 100),
                                false, v4aModule);
                    }
                } else {
                    String[] levels = preferences
                            .getString("viper4android.headphonefx.fireq.custom",
                                    "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;").split(";");
                    for (short i = 0; i < levels.length; i++) {
                        setV4AEqualizerBandLevel(i,
                                Math.round(Float.valueOf(levels[i]) * 100), false, v4aModule);
                    }
                }
                if (preferences.getBoolean("viper4android.headphonefx.fireq.enable", false)) {
                    v4aModule.setParameter(PARAM_SPKFX_FIREQ_PROCESS_ENABLED, 1);
                } else {
                    v4aModule.setParameter(PARAM_SPKFX_FIREQ_PROCESS_ENABLED, 0);
                }
            } else {
                if (m3rdEqualizerLevels != null) {
                    for (int i = 0; i < m3rdEqualizerLevels.length; i++) {
                        setV4AEqualizerBandLevel(i, Math.round(m3rdEqualizerLevels[i] * 100),
                                false, v4aModule);
                    }
                }
                if (m3rdEqualizerEnabled) {
                    v4aModule.setParameter(PARAM_SPKFX_FIREQ_PROCESS_ENABLED, 1);
                } else {
                    v4aModule.setParameter(PARAM_SPKFX_FIREQ_PROCESS_ENABLED, 0);
                }
            }

            /* ViPER's Reverberation */
            Log.i("ViPER4Android", "updateSystem(): Updating Reverberation.");
            v4aModule.setParameter(PARAM_SPKFX_REVB_ROOMSIZE, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.roomsize", "0")));
            v4aModule.setParameter(PARAM_SPKFX_REVB_WIDTH, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.roomwidth", "0")));
            v4aModule.setParameter(PARAM_SPKFX_REVB_DAMP, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.damp", "0")));
            v4aModule.setParameter(PARAM_SPKFX_REVB_WET, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.wet", "0")));
            v4aModule.setParameter(PARAM_SPKFX_REVB_DRY, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.reverb.dry", "50")));
            if (preferences.getBoolean("viper4android.headphonefx.reverb.enable", false)) {
                v4aModule.setParameter(PARAM_SPKFX_REVB_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_SPKFX_REVB_PROCESS_ENABLED, 0);
            }

            /* Convolver */
            Log.i("ViPER4Android", "updateSystem(): Updating Convolver.");
            String mConvIRFileName = preferences.getString(
                    "viper4android.headphonefx.convolver.kernel", "");
            v4aModule.setConvIRFile(mConvIRFileName, true);
            v4aModule
                    .setParameter(PARAM_SPKFX_CONV_CROSSCHANNEL, Integer.valueOf(
                            preferences.getString(
                                    "viper4android.headphonefx.convolver.crosschannel", "0")));
            if (preferences.getBoolean("viper4android.headphonefx.convolver.enable", false)) {
                v4aModule.setParameter(PARAM_SPKFX_CONV_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_SPKFX_CONV_PROCESS_ENABLED, 0);
            }

            /* Tube Simulator */
            Log.i("ViPER4Android", "updateSystem(): Shutting down tube simulator.");
            v4aModule.setParameter(PARAM_HPFX_TUBE_PROCESS_ENABLED, 0);

            /* Speaker Optimization */
            Log.i("ViPER4Android", "updateSystem(): Updating Speaker Optimizer.");
            if (preferences.getBoolean("viper4android.speakerfx.spkopt.enable", false)) {
                v4aModule.setParameter(PARAM_SPKFX_CORR_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_SPKFX_CORR_PROCESS_ENABLED, 0);
            }

            /* eXtraLoud */
            Log.i("ViPER4Android", "updateSystem(): Updating eXtraLoud.");
            v4aModule.setParameter(PARAM_SPKFX_AGC_RATIO, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.ratio", "50")));
            v4aModule.setParameter(PARAM_SPKFX_AGC_VOLUME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.playbackgain.volume", "80")));
            v4aModule.setParameter(PARAM_SPKFX_AGC_MAXSCALER, Integer.valueOf(preferences
                    .getString("viper4android.headphonefx.playbackgain.maxscaler", "400")));
            if (preferences.getBoolean("viper4android.headphonefx.playbackgain.enable", false)) {
                v4aModule.setParameter(PARAM_SPKFX_AGC_PROCESS_ENABLED, 1);
            } else {
                v4aModule.setParameter(PARAM_SPKFX_AGC_PROCESS_ENABLED, 0);
            }

            /* Limiter */
            Log.i("ViPER4Android", "updateSystem(): Updating Limiter.");
            v4aModule.setParameter(PARAM_SPKFX_OUTPUT_VOLUME, Integer.valueOf(
                    preferences.getString("viper4android.headphonefx.outvol", "100")));
            v4aModule.setParameter(PARAM_SPKFX_LIMITER_THRESHOLD, Integer.valueOf(
                    preferences.getString("viper4android.speakerfx.limiter", "100")));

            /* Master Switch */
            if (!mWorkingWith3rd) {
                boolean forceEnable = preferences.getBoolean(
                        "viper4android.global.forceenable.enable", false);
                if (forceEnable) {
                    v4aModule.setParameter(PARAM_SET_FORCEENABLE_STATUS, 1);
                } else {
                    v4aModule.setParameter(PARAM_SET_FORCEENABLE_STATUS, 0);
                }

                boolean masterControl = preferences.getBoolean(
                        "viper4android.speakerfx.enable", false);
                if (mMasterSwitchOff) {
                    masterControl = false;
                }
                if (masterControl) {
                    v4aModule.setParameter(PARAM_SET_DOPROCESS_STATUS, 1);
                } else {
                    v4aModule.setParameter(PARAM_SET_DOPROCESS_STATUS, 0);
                }
                v4aModule.mInstance.setEnabled(masterControl);
            } else {
                if (m3rdEnabled) {
                    v4aModule.setParameter(PARAM_SET_DOPROCESS_STATUS, 1);
                    v4aModule.mInstance.setEnabled(true);
                } else {
                    v4aModule.setParameter(PARAM_SET_DOPROCESS_STATUS, 0);
                    v4aModule.mInstance.setEnabled(false);
                }
            }
        }
        /******************************************************************************************************/

        /* Reset */
        if (requireReset)
            v4aModule.setParameter(PARAM_SET_RESET_STATUS, 1);

        Log.i("ViPER4Android", "System updated.");
    }
}
