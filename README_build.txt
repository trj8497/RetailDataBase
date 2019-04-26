To build the project, simply open the retailDB.iml file in the IntelliJ IDEA IDE.

You will then be able to run the Main.java file from within the IDE.

If you receive any errors asking for the H2 driver, make sure com.h2database:h2:1.4.199 is added as a maven JAR dependency (it is already, but this was a common error for us)

If you receive errors such as not being able to understand basic Java syntax, make sure you setup the project SDK.