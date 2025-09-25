package io.github.ih0r.adapter.utils;

public interface Constants {

//    This constant holds the string "python", representing the Python programming language.
//    It's used to specify the language context within the GraalVM.
    String PYTHON = "python";

//    This constant retrieves the current user directory where the Java application is running.
//    It is dynamically set using System.getProperty("user.dir").
    String USER_DIR = System.getProperty("user.dir");

//    This constant creates the full path to the python project resources
    String PROJ_RESOURCES_PATH = USER_DIR + "/src/main/" + PYTHON;
}
