# LocationProvider

LocationProvider is a tiny library that abstracts away a lot of the pain of getting a users location from the phone (without having to call location APIs), provides a fully configurable standard Builder pattern and a set of callbacks.

With the most basic usage, you give it a context and a callback and you're good to go!

**DISCLAIMER: This library needs the ACCESS_FINE_LOCATION permission, but it assumes it has been given beforehand, see sample app for more details**

[![](https://jitpack.io/v/david-serrano/locationprovider.svg)](https://jitpack.io/#david-serrano/locationprovider)

# Install instructions
1. add the jitpack dependency: 
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  ```
  
2. add a dependency to your `app/build.gradle`: 
 ```
 dependencies {
	        implementation 'com.github.david-serrano:locationprovider:1.2'
	}
  ```
  
  # Example usage:
  ```
  //create a callback
  LocationProvider.LocationCallback callback = new LocationProvider.LocationCallback() {
	    @Override
            public void locationRequestStopped(float lat, float lon) {
                //location updates stopped
            }

            @Override
            public void onNewLocationAvailable(float lat, float lon) {
                //location update
            }

            @Override
            public void locationServicesNotEnabled() {
             	//failed finding a location
            }

            @Override
            public void updateLocationInBackground(float lat, float lon) {
                //if a listener returns after the main locationAvailable callback, it will go here
            }

            @Override
            public void networkListenerInitialised() {
              //when the library switched from GPS only to GPS & network
            }
        };
	
	//initialise an instance with the two required parameters
    LocationProvider locationProvider = new LocationProvider.Builder()
	.setContext(this)
	.setListener(callback)
	.create();
	
	//start getting location
    locationProvider.requestLocation();
```  
  # How it works:
 The library provides a standard Builder pattern with a few configurable options but uses default values if none are set.
 In the most standard use case it will:
 1. Look for last known locations in order the following order `passive`, `gps`, `network`. If those return a value `onNewLocationAvailable` will be called.
 2. Initialise a GPS listener or network listener depending on availability of service (high accuracy vs battery saving for example)
 3. Return location based on available providers:
   - If GPS initialised but taking too long, start a network listener after X (configurable) seconds.
   - If only network initialised, some phones fail to update the listener but update the passive values correctly, so after Y (configurable) seconds, try the last known locations again.
 4. Return values for GPS/network when available and shut down self, call back to listener.
 
 # License
 [MIT License](./LICENSE)
