# Bosch IoT Things - Hello World Example

This example shows how to create and use the Java Integration Client for managing your first Hello World Thing.
For detailed instructions see: <a href="https://imbvl4vm328.bosch-si.com/cr/doku.php?id=005_dev_guide:tutorial:000_hello_world">here</a>

## Configure your settings if needed

Create or adjust file "src/main/resources/config.properties"

```
#http.proxyHost=### your http proxy host, if you need one ###
#http.proxyPort=### your http proxy host, if you need one ###
#http.proxyPrincipal=### your http proxy principal (user), if you need one ###
#http.proxyPassword=### your http proxy password, if you need one ###
```

## Configure your Solution ID and UserID  
Set your Solution ID in file "src/main/java/.../HelloWorld.java"
```
public static final String SOLUTION_ID = <your-solution-id>;
```
```
public static  final String USER_ID = "<your-user-id>";
```

## Create an Empty Thing and add ThingID
Set your ThingID in "src/test/java/.../HelloWorldLauncher.java" 
```
final String thingId = <your-thing-id>; 
```

## Build

Use the following maven command to build the project:
```
mvn clean install
```

## Usage
Run the project to update the attributes of the specified thing.


## License

See the cr-examples top level README.md file for license details.