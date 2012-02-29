package io.socket.testutils;

import java.util.Collections;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;


// See http://stackoverflow.com/questions/1444314/how-can-i-make-my-junit-tests-run-in-random-order

public class RandomBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner {

    public RandomBlockJUnit4ClassRunner(Class<?> klass)
    		throws InitializationError {
    	super(klass);
    }
   
    
    @Override
	protected java.util.List<org.junit.runners.model.FrameworkMethod> computeTestMethods() {
    	java.util.List<org.junit.runners.model.FrameworkMethod> methods = super.computeTestMethods();
    	Collections.shuffle(methods);
    	return methods;
    }

}