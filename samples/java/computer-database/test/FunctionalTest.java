import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.flash;
import static play.test.Helpers.redirectLocation;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import play.mvc.Result;

@RunWith(FunctionalTest.TheRunner.class)
public class FunctionalTest {

	public static class TheRunner extends BlockJUnit4ClassRunner {

		public TheRunner(Class<?> klass) throws InitializationError {
			super(klass);
		}

		@Override
		protected Statement methodBlock(FrameworkMethod method) {
			Statement statement = super.methodBlock(method);
			return fakeApplicationStatement(statement);
		}

		private Statement fakeApplicationStatement(final Statement base) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					running(fakeApplication(), new Runnable() {
						public void run() {
							try {
								base.evaluate();
							} catch (Throwable e) {
								if (e instanceof RuntimeException) {
									throw (RuntimeException)e;
								}
								throw new RuntimeException(e);
							}
						}
					});
				}
			};
		}

	}

    @Test
    public void redirectHomePage() {
               Result result = callAction(controllers.routes.ref.Application.index());

               assertThat(status(result)).isEqualTo(SEE_OTHER);
               assertThat(redirectLocation(result)).isEqualTo("/computers");
    }

    @Test
    public void listComputersOnTheFirstPage() {
               Result result = callAction(controllers.routes.ref.Application.list(0, "name", "asc", ""));

               assertThat(status(result)).isEqualTo(OK);
               assertThat(contentAsString(result)).contains("574 computers found");
    }

    @Test
    public void filterComputerByName() {
               Result result = callAction(controllers.routes.ref.Application.list(0, "name", "asc", "Apple"));

               assertThat(status(result)).isEqualTo(OK);
               assertThat(contentAsString(result)).contains("13 computers found");
    }

    @Test
    public void createANewComputer() {
                Result result = callAction(controllers.routes.ref.Application.save());

                assertThat(status(result)).isEqualTo(BAD_REQUEST);

                Map<String,String> data = new HashMap<String,String>();
                data.put("name", "FooBar");
                data.put("introduced", "badbadbad");
                data.put("company.id", "1");

                result = callAction(
                    controllers.routes.ref.Application.save(),
                    fakeRequest().withFormUrlEncodedBody(data)
                );

                assertThat(status(result)).isEqualTo(BAD_REQUEST);
                assertThat(contentAsString(result)).contains("<option value=\"1\" selected>Apple Inc.</option>");
                assertThat(contentAsString(result)).contains("<input type=\"text\" id=\"introduced\" name=\"introduced\" value=\"badbadbad\" >");
                assertThat(contentAsString(result)).contains("<input type=\"text\" id=\"name\" name=\"name\" value=\"FooBar\" >");

                data.put("introduced", "2011-12-24");

                result = callAction(
                    controllers.routes.ref.Application.save(),
                    fakeRequest().withFormUrlEncodedBody(data)
                );

                assertThat(status(result)).isEqualTo(SEE_OTHER);
                assertThat(redirectLocation(result)).isEqualTo("/computers");
                assertThat(flash(result).get("success")).isEqualTo("Computer FooBar has been created");

                result = callAction(controllers.routes.ref.Application.list(0, "name", "asc", "FooBar"));
                assertThat(status(result)).isEqualTo(OK);
                assertThat(contentAsString(result)).contains("One computer found");

    }

}
