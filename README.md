# LocationProvider

An implementation that returns a user location with the phones local data, no API's, no fuss.

**DISCLAIMER: This library needs the COARSE and FINE permissions, but it assumes they have been given beforehand**

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
	        implementation 'com.github.david-serrano:locationprovider:master-SNAPSHOT'
	}
  ```
  
  # Example usage:
  ```
  //create a callback
  LocationProvider.LocationCallback callback = new LocationProvider.LocationCallback() {
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
 3. 
 3.1 If GPS initialised but taking too long, start a network listener after X (configurable) seconds.
 3.2 If only network initialised, some phones fail to update the listener but update the passive values correctly, so after Y (configurable) seconds, try the last known locations again.
 4. Return values for GPS/network when available and shut down self.
 
 # License
 [MIT License](./LICENSE)
