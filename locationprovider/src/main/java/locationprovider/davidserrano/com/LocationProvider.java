package locationprovider.davidserrano.com;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;


public class LocationProvider {

    public interface LocationCallback {
        void onNewLocationAvailable(float latitude, float longitude);

        void locationServicesNotEnabled();

        void updateLocationInBackground(float latitude, float longitude);

        void networkListenerInitialised();

        void locationRequestStopped();
    }

    private static LocationManager locationManager;
    private static LocationListener gpsUpdatesListener;
    private static LocationListener networkUpdatesListener;
    private static boolean isFirstToReturn = true;
    private static LocationCallback locationCallback;
    private static CountDownTimer networkTimer;
    private static CountDownTimer gpsTimer;
    private static boolean isRequestingLocationActive = false;

    private LocationCallback callback;
    private long gpsTimeoutMillis;
    private long networkTimeoutMillis;
    private int minimumGpsUpdateTime;
    private int minimumGpsUpdateDistance;
    private int minimumNetworkUpdateTime;
    private int minimumNetworkUpdateDistance;
    private boolean loggingEnabled;
    private Context context;

    private LocationProvider(final Builder builder) {
        context = builder.context;
        callback = builder.callback;
        gpsTimeoutMillis = builder.gpsTimeoutMillis;
        networkTimeoutMillis = builder.networkTimeoutMillis;
        minimumGpsUpdateTime = builder.minimumGpsUpdateTime;
        minimumGpsUpdateDistance = builder.minimumGpsUpdateDistance;
        minimumNetworkUpdateDistance = builder.minimumNetworkUpdateDistance;
        minimumNetworkUpdateTime = builder.minimumNetworkUpdateTime;
        loggingEnabled = builder.loggingEnabled;
    }

    @SuppressLint("MissingPermission")
    public void requestLocation() {
        if (getIsRequestingLocation()) {
            removeUpdates();
        }

        isFirstToReturn = true;
        isRequestingLocationActive = true;

        outputLog("starting location service");

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationCallback = callback;

        Location lastKnownPassiveLocation = getLastKnownLocation(context, LocationManager.PASSIVE_PROVIDER);
        if (lastKnownPassiveLocation != null) {
            outputLog("valid passive provider - callback");

            updateLocation((float) lastKnownPassiveLocation.getLatitude(), (float) lastKnownPassiveLocation.getLongitude());
        } else {
            outputLog("invalid passive provider");

            Location lastKnownGPS = getLastKnownLocation(context, LocationManager.GPS_PROVIDER);
            if (lastKnownGPS != null) {
                outputLog("invalid passive but valid gps - callback");

                updateLocation((float) lastKnownGPS.getLatitude(), (float) lastKnownGPS.getLongitude());
            } else {
                outputLog("invalid gps provider");

                Location lastKnownNetwork = getLastKnownLocation(context, LocationManager.NETWORK_PROVIDER);
                if (lastKnownNetwork != null) {
                    outputLog("invalid passive and gps but valid network - callback");
                    updateLocation((float) lastKnownNetwork.getLatitude(), (float) lastKnownNetwork.getLongitude());
                } else {
                    outputLog("invalid network provider");
                }
            }
        }

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGPSEnabled) {
            gpsTimer = new CountDownTimer(gpsTimeoutMillis, gpsTimeoutMillis) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    networkUpdatesListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            outputLog("network returned");
                            updateLocation((float) location.getLatitude(), (float) location.getLongitude());
                            removeUpdates();
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                        }
                    };
                    if (isNetworkEnabled) {
                        outputLog("GPS timer finished - Network enabled, listening for updates");
                        startUpdates(context, LocationManager.NETWORK_PROVIDER, minimumNetworkUpdateDistance, minimumNetworkUpdateTime, networkUpdatesListener);
                    }
                }
            };
            gpsUpdatesListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    outputLog("GPS returned");
                    updateLocation((float) location.getLatitude(), (float) location.getLongitude());
                    if (gpsTimer != null) {
                        gpsTimer.cancel();
                    }
                    removeUpdates();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };

            outputLog("GPS enabled, listening for updates");
            gpsTimer.start();
            startUpdates(context, LocationManager.GPS_PROVIDER, minimumGpsUpdateTime, minimumGpsUpdateDistance, gpsUpdatesListener);
        } else if (isNetworkEnabled) {

            networkUpdatesListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    outputLog("network returned");
                    updateLocation((float) location.getLatitude(), (float) location.getLongitude());
                    if (networkTimer != null) {
                        networkTimer.cancel();
                    }
                    removeUpdates();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };
            outputLog("Network enabled, listening for updates");
            startUpdates(context, LocationManager.NETWORK_PROVIDER, minimumNetworkUpdateTime, minimumNetworkUpdateDistance, networkUpdatesListener);

            networkTimer = new CountDownTimer(networkTimeoutMillis, networkTimeoutMillis) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    outputLog("timer finished");
                    Location lastKnownPassiveLocation = getLastKnownLocation(context, LocationManager.PASSIVE_PROVIDER);
                    removeUpdates();
                    if (lastKnownPassiveLocation != null) {
                        outputLog("valid passive provider - callback");

                        updateLocation((float) lastKnownPassiveLocation.getLatitude(), (float) lastKnownPassiveLocation.getLongitude());
                    } else {
                        outputLog("timer finished, invalid passive, clearing & restarting");

                        requestLocation();
                    }
                }
            };
            networkTimer.start();
        } else {
            outputLog("No providers are available");
            callback.locationServicesNotEnabled();
        }
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation(Context context, String provider) {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
        return locationManager.getLastKnownLocation(provider);
    }

    @SuppressLint("MissingPermission")
    private void startUpdates(Context context, String provider, int time, int distance, LocationListener listener) {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            locationCallback.networkListenerInitialised();
        }

        locationManager.requestLocationUpdates(provider, time, distance, listener);
    }

    private void updateLocation(float lat, float lon) {
        if (isFirstToReturn) {
            outputLog("first callback");
            locationCallback.onNewLocationAvailable(lat, lon);
            isFirstToReturn = false;
        } else {
            outputLog("background update");
            locationCallback.updateLocationInBackground(lat, lon);
        }
    }

    private void outputLog(String log) {
        if (loggingEnabled) {
            Log.d("LocationProvider", log);
        }
    }

    private boolean getIsRequestingLocation() {
        return isRequestingLocationActive;
    }

    @SuppressLint("MissingPermission")
    private void removeUpdates() {
        outputLog("attempting to remove listeners");

        if (gpsUpdatesListener != null && locationManager != null) {
            outputLog("removed gps listener");
            locationManager.removeUpdates(gpsUpdatesListener);
            gpsUpdatesListener = null;
        }
        if (networkUpdatesListener != null && locationManager != null) {
            outputLog("removed network listener");
            locationManager.removeUpdates(networkUpdatesListener);
            networkUpdatesListener = null;
        }
        if (networkTimer != null) {
            outputLog("removed network timer");
            networkTimer.cancel();
            networkTimer = null;
        }
        if (gpsTimer != null) {
            outputLog("removed GPS timer");
            gpsTimer.cancel();
            gpsTimer = null;
        }
        if (locationManager != null) {
            outputLog("removed location manager");
            locationManager = null;
        }

        isRequestingLocationActive = false;
        callback.locationRequestStopped();
    }

    public static class Builder {
        private LocationCallback callback;
        private long gpsTimeoutMillis = 7000;
        private long networkTimeoutMillis = 3000;
        private int minimumGpsUpdateTime = 100;
        private int minimumGpsUpdateDistance = 100;
        private int minimumNetworkUpdateTime = 0;
        private int minimumNetworkUpdateDistance = 0;
        private boolean loggingEnabled = true;
        private Context context;

        public Builder setListener(LocationCallback callback) {
            this.callback = callback;
            return this;
        }

        public Builder setGPSTimeout(long gpsTimeoutMillis) {
            this.gpsTimeoutMillis = gpsTimeoutMillis;
            return this;
        }

        public Builder setMinimumGpsUpdateTime(int minimumGpsUpdateTime) {
            this.minimumGpsUpdateTime = minimumGpsUpdateTime;
            return this;
        }

        public Builder setMinimumGpsUpdateDistance(int minimumGpsUpdateDistance) {
            this.minimumGpsUpdateDistance = minimumGpsUpdateDistance;
            return this;
        }

        public Builder setMinimumNetworkUpdateTime(int minimumNetworkUpdateTime) {
            this.minimumNetworkUpdateTime = minimumNetworkUpdateTime;
            return this;
        }

        public Builder setMinimumNetworkUpdateDistance(int minimumNetworkUpdateDistance) {
            this.minimumNetworkUpdateDistance = minimumNetworkUpdateDistance;
            return this;
        }

        public Builder setLoggingEnabled(boolean loggingEnabled) {
            this.loggingEnabled = loggingEnabled;
            return this;
        }

        public Builder setNetworkTimeout(long networkTimeoutMillis) {
            this.networkTimeoutMillis = networkTimeoutMillis;
            return this;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public LocationProvider create() {
            if (this.context == null) {
                try {
                    throw new Exception("Context needs to be passed in");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (this.callback == null) {
                try {
                    throw new Exception("No callback provided, do you expect updates?");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return new LocationProvider(this);
        }
    }

}
