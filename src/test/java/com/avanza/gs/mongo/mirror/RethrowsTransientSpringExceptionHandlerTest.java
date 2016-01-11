/*
 * Copyright 2015 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.avanza.gs.mongo.mirror;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import com.avanza.gs.mongo.mirror.DocumentWriteExceptionHandler;
import com.avanza.gs.mongo.mirror.RethrowsTransientSpringExceptionHandler;
import com.avanza.gs.mongo.mirror.TransientDocumentWriteException;


public class RethrowsTransientSpringExceptionHandlerTest {

	private DocumentWriteExceptionHandler handler = new RethrowsTransientSpringExceptionHandler();

	@Test(expected = TransientDocumentWriteException.class)
	public void throwsOnTransientException() throws Exception {
		handler.handleException(new DataAccessResourceFailureException(""), "");
	}

	@Test
	public void doesNotThrowOnNonTransientException() throws Exception {
		try {
			handler.handleException(new RuntimeException(), "");
		} catch (RuntimeException e) {
			fail("Exception should not be thrown");
		}
	}

	@Test	
	public void throwsOnTransientExceptionByErrorMessage() throws Exception {		
		List<String> transientErrorMessages = Arrays.asList("No replica set members available for query with", "not master");
		for (String message : transientErrorMessages) {
			try {
				handler.handleException(new RuntimeException(message), "");
				fail("Exception should not be thrown for exception with message " + message);
			} catch (TransientDocumentWriteException e) {
			}
		}
	}

}