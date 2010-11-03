package net.jxta.test.util;

import org.jmock.Mockery;
import org.jmock.lib.AssertionErrorTranslator;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * JUnit 4.7 based mockery for JMock - this code is taken from issue
 * JMOCK-237 in the JMock issue tracker. This code is not yet
 * available in a released version of JMock, but is essential for
 * us to mix JMock with rule based tests.
 */
public class JUnitRuleMockery extends Mockery implements MethodRule {

	public JUnitRuleMockery() {
	   setExpectationErrorTranslator(AssertionErrorTranslator.INSTANCE);
	}
	
	public Statement apply(final Statement base, final FrameworkMethod method,
			Object target) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
					base.evaluate();
				} catch(Throwable exp) {
					if(!isExceptionExpectedByTestMethod(exp, method)) {
						throw exp;
					}
				}
				assertIsSatisfied();
			}
		};
	}

	protected boolean isExceptionExpectedByTestMethod(Throwable exp, FrameworkMethod method) {
		Test test = method.getAnnotation(Test.class);
		return test.expected().equals(exp.getClass());
	}

}
