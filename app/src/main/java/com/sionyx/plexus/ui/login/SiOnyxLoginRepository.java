package com.sionyx.plexus.ui.login;

import static com.amazonaws.mobile.client.UserState.SIGNED_OUT;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignOutOptions;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.ForgotPasswordResult;
import com.amazonaws.mobile.client.results.ListDevicesResult;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignInState;
import com.amazonaws.mobile.client.results.Tokens;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.sionyx.plexus.ui.api.interfaces.CloudInterface;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class SiOnyxLoginRepository {
    private static final String TAG = "SiOnyxLoginRepository";
    private final String message_Token_Invalid = "Access Token invalid";
    private final String NoInternet = "No address associated with hostname";
    private final Application application;
    private static SiOnyxLoginRepository siOnyxLoginRepository;
    private GlobalSignOutListener globalSignOutListener = null;

    public SiOnyxLoginRepository(Application application) {
        this.application = application;
    }

    public static SiOnyxLoginRepository getInstance(Application application) {
        if (siOnyxLoginRepository == null) {
            siOnyxLoginRepository = new SiOnyxLoginRepository(application);
        }
        return siOnyxLoginRepository;
    }

    void initializeAWS(Context context, final CloudInterface cloudInterface) {
        AWSMobileClient.getInstance().initialize(context, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails userStateDetails) {
                Log.e(TAG, "initialize process 1" + " " + userStateDetails.getUserState() + " ");

                if (userStateDetails.getUserState() == SIGNED_OUT) {
                    cloudInterface.onSuccess("", "SIGNED_OUT");
                    return;
                }
                switch (userStateDetails.getUserState()) {
                    case SIGNED_IN:
                        try {
                            String tokenString = AWSMobileClient.getInstance().getTokens().getIdToken().getTokenString();
                            cloudInterface.onSuccess(tokenString, "SIGNED_IN");
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                            Log.e(TAG, " getToken exce " + e.getMessage());
                            exceptionCheck(e, cloudInterface);
                        } catch (Exception e) {
                            e.printStackTrace();
                            exceptionCheck(e, cloudInterface);
                        }
                        break;
                    case SIGNED_OUT:
                        cloudInterface.onSuccess(null, "SIGNED_OUT");
                        break;
                    case SIGNED_OUT_FEDERATED_TOKENS_INVALID:
                    case SIGNED_OUT_USER_POOLS_TOKENS_INVALID:
                        cloudInterface.onSuccess(null, "TOKENS_INVALID");
                        globalSignOutErrorCheck(message_Token_Invalid, cloudInterface);
                        break;
                    case GUEST:
                        cloudInterface.onSuccess(null, "GUEST");
                        break;
                    case UNKNOWN:
                        cloudInterface.onSuccess(null, "UNKNOWN");
                        break;
                    default:
                        signOutAwscall();
                        break;

                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, " initializeAWS error" + " " + e.getMessage());
                String s = formatException(e);
                if (s != null) {
                    if (s.contains(NoInternet)) {
                        cloudInterface.onFailure(s);
                    } else {
                        globalSignOutErrorCheck(s, cloudInterface);
                    }
                }
            }
        });
    }

    private void exceptionCheck(Exception e, CloudInterface cloudInterface) {
        String s = formatException(e);
        globalSignOutErrorCheck(s, cloudInterface);
    }

    @SuppressLint("StaticFieldLeak")
    void getDeviceList(final CloudInterface cloudInterface) {
        String signedDeviceKey = "";
        try {
            signedDeviceKey = AWSMobileClient.getInstance().getDeviceOperations().get().getDeviceKey();
            Log.e(TAG, "@@@@device getCurrent key success" + " " + signedDeviceKey);
        } catch (Exception e) {
            e.printStackTrace();
            String s = formatException(e);
            if (!s.contains("Device does not exist.")) {
                globalSignOutErrorCheck(s, cloudInterface);
            } else {
                cloudInterface.onFailure("ALREADY SIGNED_OUT");
            }
            Log.e(TAG, "@@@@device getCurrent key error" + " " + s);
        }
        if (signedDeviceKey.contains("Device does not exist.")) {// for checking error case of getDevice key
            signOutAwscall();
            globalSignOutListener.signOutSuccess("ALREADY SIGNED_OUT");
        } else {
            String finalSignedDeviceKey = signedDeviceKey;
            AWSMobileClient.getInstance().getDeviceOperations().list(new Callback<ListDevicesResult>() {
                @Override
                public void onResult(ListDevicesResult result) {
                    if (result.getDevices().isEmpty()) {
                        cloudInterface.onSuccess(finalSignedDeviceKey, "User Registered Successfully");
                    } else if (result.getDevices().size() == 1) {
                        String deviceKey = result.getDevices().get(0).getDeviceKey();
                        if (finalSignedDeviceKey.equalsIgnoreCase(deviceKey))
                            cloudInterface.onSuccess(finalSignedDeviceKey, "User Registered Successfully");
                        else
                            cloudInterface.onSuccess(finalSignedDeviceKey, "User Already Registered");
                    } else if (result.getDevices().size() > 1) {
                        cloudInterface.onSuccess(finalSignedDeviceKey, "User Already Registered");
                    }
                }

                @Override
                public void onError(Exception e) {
                    String s = formatException(e);
                    if (s.contains(NoInternet)) {
                        cloudInterface.onFailure(siOnyxLoginRepository.NoInternet);
                    } else {
                        cloudInterface.onFailure(s);
                    }
                }
            });
        }
    }

    void signOutAlreadyLogin(final Map<String, String> attributeMap, final CloudInterface cloudInterface) {
        AWSMobileClient.getInstance().updateUserAttributes(attributeMap, new Callback<List<UserCodeDeliveryDetails>>() {
            @Override
            public void onResult(List<UserCodeDeliveryDetails> result) {
                // User attributes updated successfully
                cloudInterface.onSuccess(result.toString(), result.toString());
                Log.d(TAG, "updateUserAttributes: " + result);
                // here global signed out
                SignOutOptions options = SignOutOptions.builder()
                        .signOutGlobally(true)// Sign out from all devices
                        .build();
                AWSMobileClient.getInstance().signOut(options, new Callback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        cloudInterface.onSuccess(result.toString(), result.toString());
                        Log.d(TAG, "signOut error : " + result.toString());
                    }

                    @Override
                    public void onError(Exception e) {
                        cloudInterface.onFailure(e.getMessage());
                        Log.d(TAG, "signOut error : " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                // Error updating user attributes
                Log.d(TAG, "onError: " + e.getMessage());
            }
        });
    }

    void globalLogout(final Map<String, String> attributeMap, final CloudInterface cloudInterface) {
        AWSMobileClient.getInstance().updateUserAttributes(attributeMap, new Callback<List<UserCodeDeliveryDetails>>() {
            @Override
            public void onResult(List<UserCodeDeliveryDetails> result) {
                // User attributes updated successfully
                Log.d(TAG, "updateUserAttributes: " + result);
                // here global signed out
                SignOutOptions options = SignOutOptions.builder()
                        .signOutGlobally(true)// Sign out from all devices
                        .build();
                AWSMobileClient.getInstance().signOut(options, new Callback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        cloudInterface.onSuccess("", result.toString());
                        Log.d(TAG, "signOut success : " + result);
                    }

                    @Override
                    public void onError(Exception e) {
                        cloudInterface.onFailure(e.getMessage());
                        Log.d(TAG, "signOut error : " + e.getMessage());
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                // Error updating user attributes
                cloudInterface.onFailure(e.getMessage());
                Log.d(TAG, "onError: " + e.getMessage());
            }
        });
    }

    void signOut(final String deviceId, final CloudInterface cloudInterface) {
        AWSMobileClient.getInstance().getDeviceOperations().forget(deviceId, new Callback<Void>() {
            @Override
            public void onResult(Void result) {
                Log.e(TAG, "@@@@remove device list success" + " " + deviceId);
                signOutAwscall();
                cloudInterface.onSuccess(deviceId, "SIGNED_OUT");
            }

            @Override
            public void onError(Exception e) {
                String s = formatException(e);
                if (s.contains("Device does not exist.")) {
                    cloudInterface.onSuccess(deviceId, "ALREADY SIGNED_OUT");
                } else if (s.contains(NoInternet)) {
                    cloudInterface.onFailure(siOnyxLoginRepository.NoInternet);
                } else {
                    globalSignOutErrorCheck(s, cloudInterface);
                }
            }
        });
    }

    void signIN(String username, String password, final CloudInterface cloudInterface) {
        AWSMobileClient.getInstance().signIn(username, password, null, new Callback<SignInResult>() {
            @Override
            public void onResult(SignInResult result) {
                Log.e(TAG, "signIN onResult" + " " + result.getSignInState());
                final String[] tokenString = new String[1];
                if (result.getSignInState() == SignInState.DONE) {
                    try {
                        AWSMobileClient.getInstance().getTokens(new Callback<Tokens>() {
                            @Override
                            public void onResult(Tokens result) {
                                tokenString[0] = result.getIdToken().getTokenString();
                                cloudInterface.onSuccess(tokenString[0], "Success");
                                Log.e(TAG, " tokenString " + " " + tokenString[0]);
                            }

                            @Override
                            public void onError(Exception e) {
                                cloudInterface.onFailure(e.getMessage());
                                Log.e(TAG, " tokenString onError" + " " + e.getMessage());
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        String s = formatException(e);
                        if (s.contains(NoInternet)) {
                            cloudInterface.onFailure(siOnyxLoginRepository.NoInternet);
                        } else {
                            globalSignOutErrorCheck(s, cloudInterface);
                        }
                    }
                } else {
                    cloudInterface.onFailure("Unsupported sign-in confirmation:");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "SIGNIN onError " + " " + e.getMessage());
                String s = formatException(e);
                if (s.contains(NoInternet)) {
                    cloudInterface.onFailure(siOnyxLoginRepository.NoInternet);
                } else {
                    globalSignOutErrorCheck(s, cloudInterface);
                }
            }
        });
    }

    void confirmForgotPassword(String newpwd, String confimationcode, final CloudInterface cloudInterface) {
        Log.e(TAG, "confirmForgotPassword" + " " + newpwd + " " + confimationcode);
        // I commented this due to AWSMobileClient is deprecated.
        //Alternative use : use Amplify.Auth.confirmResetPassword() from the AWS Amplify SDK. we need update this
//        AWSMobileClient.getInstance().confirmForgotPassword(newpwd, confimationcode, new Callback<ForgotPasswordResult>() {
//            @Override
//            public void onResult(ForgotPasswordResult result) {
//                cloudInterface.onSuccess(null, "SIGNED_IN");
//            }
//
//            @Override
//            public void onError(Exception e) {
//                String s = formatException(e);
//                if (s.contains(NoInternet)) {
//                    cloudInterface.onFailure(siOnyxLoginRepository.NoInternet);
//                } else {
//                    globalSignOutErrorCheck(s, cloudInterface);
//                }
//            }
//        });
    }
    private void globalSignOutErrorCheck(String s, CloudInterface cloudInterface) {
        Log.e(TAG, "globalSignoutErrorCheck " + " " + s);
        String message_Token_Revoked = "Access Token has been revoked";
        if (s.replaceAll("\\s+", "").equalsIgnoreCase(message_Token_Revoked.replaceAll("\\s+", ""))) {
            signOutAwscall();
            globalSignOutListener.signOutSuccess(message_Token_Revoked);
        } else if (s.replaceAll("\\s+", "").equalsIgnoreCase(message_Token_Invalid.replaceAll("\\s+", "")) || s.contains("getTokens does not support retrieving tokens for federated sign-in") || s.contains("No cached session.") || s.contains("getTokens does not support retrieving tokens while signed-out")) {
            signOutAwscall();
            globalSignOutListener.signOutSuccess(message_Token_Invalid);
        } else {
            cloudInterface.onFailure(s);
        }
    }

    public void signOutAwscall() {
        Log.e(TAG, "signOut Called ");
        AWSMobileClient.getInstance().signOut();
    }

    private String formatException(Exception exception) {
        String formattedString = "Internal Error";
        Log.e("AWS", " -- Error: " + exception.toString());
        Log.getStackTraceString(exception);
        String temp = exception.getMessage();
        if (temp != null && temp.length() > 0) {
            formattedString = temp.split("\\(")[0];
            if (temp != null && temp.length() > 0) {
                return formattedString;
            }
        }
        return formattedString;
    }

    public interface GlobalSignOutListener {
        void signOutSuccess(String msg);
    }
}
