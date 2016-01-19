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
package com.avanza.ymer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import com.gigaspaces.datasource.SpaceDataSource;
import com.gigaspaces.sync.SpaceSynchronizationEndpoint;
import com.mongodb.ReadPreference;
/**
 * @author Elias Lindholm (elilin)
 *
 */
public final class YmerFactory {
	
	private MirrorExceptionListener exceptionListener = new MirrorExceptionListener() {
		@Override
		public void onMirrorException(Exception e, MirrorOperation failedOperation, Object[] failedObjects) {}
	};
	private ReadPreference readPreference = ReadPreference.primary();
	private boolean exportExceptionHandleMBean = true;
	
	private final MirroredDocuments mirroredDocuments;
	private final MongoConverterFactory mongoConverterFactory;
	private final MongoDbFactory mongoDbFactory;
	
	@Autowired
	public YmerFactory(MongoDbFactory mongodDbFactory, MongoConverterFactory mongoConverterFactory, MirroredDocumentDefinitions mirroredDocumentDefinitions) {
		this.mongoDbFactory = mongodDbFactory;
		this.mongoConverterFactory = mongoConverterFactory;
		this.mirroredDocuments = new MirroredDocuments(mirroredDocumentDefinitions.getDefinitions());
	}
	
	/**
	 * Defines whether an ExceptionHandlerMBean should be exported. The ExceptionHandlerMBean allows setting the SpaceSynchronizationEndpoint
	 * in a state where the bulk of operations is discarded if a failure occurs during synchronization. The default behavior is to keep a filed bulk
	 * operation first in the queue and wait for a defined interval before running a new attempt to synchronize the bulk. This blocks all 
	 * subsequent synchronization operations until the bulk succeeds. 
	 * 
	 * Default is "true"
	 * 
	 * @param exportExceptionHandleMBean
	 */
	public void setExportExceptionHandlerMBean(boolean exportExceptionHandleMBean) {
		this.exportExceptionHandleMBean = exportExceptionHandleMBean;
	}
	
	/**
	 * Sets a MirrorExceptionListener (optional). <p>
	 * 
	 * @param exceptionListener
	 */
	public void setExceptionListener(MirrorExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	public SpaceDataSource createSpaceDataSource() {
		return new YmerSpaceDataSource(createSpaceMirrorContext());
	}

	public SpaceSynchronizationEndpoint createSpaceSynchronizationEndpoint() {
		YmerSpaceSynchronizationEndpoint ymerSpaceSynchronizationEndpoint = new YmerSpaceSynchronizationEndpoint(createSpaceMirrorContext());
		if (this.exportExceptionHandleMBean) {
			ymerSpaceSynchronizationEndpoint.registerExceptionHandlerMBean();
		}
		return ymerSpaceSynchronizationEndpoint;
	}
	
	private SpaceMirrorContext createSpaceMirrorContext() {
		MongoConverter mongoConverter = mongoConverterFactory.createMongoConverter();
		DocumentDb documentDb = DocumentDb.mongoDb(this.mongoDbFactory.getDb(), readPreference);
		DocumentConverter documentConverter = DocumentConverter.mongoConverter(mongoConverter);
		// Set the event publisher to null to avoid deadlocks when loading data in parallel
		if (mongoConverter.getMappingContext() instanceof ApplicationEventPublisherAware) {
			((ApplicationEventPublisherAware)mongoConverter.getMappingContext()).setApplicationEventPublisher(null);
		}
		SpaceMirrorContext mirrorContext = new SpaceMirrorContext(mirroredDocuments, documentConverter, documentDb, exceptionListener);
		return mirrorContext;
	}
	
}
