# LocationProvider

An implementation that returns a user location with the phones local data, no API's no fuss.

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
	        implementation 'com.github.david-serrano:locationprovider:-SNAPSHOT'
	}
  ```
