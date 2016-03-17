# Bosch IoT Things - Hello World Example

This example shows how to create and use the Java Integration Client for managing your first Hello World Thing.
For detailed instructions see: <a href="https://m2m.bosch-si.com/cr/doku.php?id=005_dev_guide:tutorial:000_hello_world">here</a>

## Configure your Solution ID and UserID  
Set your IDs and keystore passwords in the file "src/main/java/.../HelloWorld.java"
```
public static final String SOLUTION_ID = <your-solution-id>;
public static  final String USER_ID = "<your-user-id>";
public static final String KEYSTORE_PASSWORD = "<your-keystore-password>";
public static final String ALIAS_PASSWORD = "<your-alias-password>";
```

## Usage
Run "src/main/java/.../HelloWorld.java" to create and update a thing.

## License
See the cr-examples top level README.md file for license details.