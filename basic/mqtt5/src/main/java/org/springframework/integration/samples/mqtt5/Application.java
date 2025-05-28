/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.samples.mqtt5;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.integration.stream.CharacterStreamReadingMessageSource;
import org.springframework.messaging.MessageHandler;

import java.nio.charset.StandardCharsets;

/**
 * Starts the Spring Context and will initialize the Spring Integration message flow.
 *
 * @author Gary Russell
 *
 */
@SpringBootApplication
public class Application {

	private static final Log LOGGER = LogFactory.getLog(Application.class);

	/**
	 * Load the Spring Integration Application Context
	 *
	 * @param args - command line arguments
	 */
	public static void main(final String... args) {

		LOGGER.info("\n========================================================="
				+ "\n                                                         "
				+ "\n          Welcome to Spring Integration!                 "
				+ "\n                                                         "
				+ "\n    For more information please visit:                   "
				+ "\n    https://spring.io/projects/spring-integration        "
				+ "\n                                                         "
				+ "\n=========================================================");

		LOGGER.info("\n========================================================="
				+ "\n                                                          "
				+ "\n    This is the MQTT Sample -                             "
				+ "\n                                                          "
				+ "\n    Please enter some text and press return. The entered  "
				+ "\n    Message will be sent to the configured MQTT topic,    "
				+ "\n    then again immediately retrieved from the Message     "
				+ "\n    Broker and ultimately printed to the command line.    "
				+ "\n                                                          "
				+ "\n=========================================================");

		SpringApplication.run(Application.class, args);
	}

	@Bean
	public MqttConnectionOptions mqttConnectionOptions() {
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setServerURIs(new String[]{ "tcp://localhost:1883" });
		options.setUserName("guest");
		options.setPassword("guest".getBytes(StandardCharsets.UTF_8));

		// MQTT 5.0 Options
		options.setSessionExpiryInterval(60L);
		options.setMaximumPacketSize(1024L * 1024L);
		options.setReceiveMaximum(10);
		options.setRequestResponseInfo(true);
		options.setTopicAliasMaximum(10);
		return options;
	}

	// publisher

	@Bean
	public IntegrationFlow mqttOutFlow() {
		return IntegrationFlow.from(CharacterStreamReadingMessageSource.stdin(),
						e -> e.poller(Pollers.fixedDelay(1000)))
				.transform(p -> p + " sent to MQTT")
				.handle(mqttOutbound())
				.get();
	}

	@Bean
	public MessageHandler mqttOutbound() {
		Mqttv5PahoMessageHandler messageHandler = new Mqttv5PahoMessageHandler(mqttConnectionOptions(), "siSamplePublisher");
		messageHandler.setAsync(true);
		messageHandler.setDefaultTopic("siSampleTopic");

		// MQTT 5.0 Options
		messageHandler.setDefaultQos(1); // QoS 1 설정
		messageHandler.setDefaultRetained(false); // Retained 플래그 (기본 false)

		// MQTT 5.0 Options
//		Mqtt5PublishBuilderCustomizer customizer = builder -> {
//			builder.contentType("text/plain"); // 콘텐츠 타입 설정
//			builder.payloadFormatIndicator(MqttProperties.PayloadFormatIndicator.UTF_8); // Payload Format
//			builder.userProperties().add("source", "spring-integration"); // User Property 추가
//		};
//
//		messageHandler.setPublishBuilderCustomizer(customizer);

		return messageHandler;
	}

	// consumer

	@Bean
	public IntegrationFlow mqttInFlow() {

		//TODO 함수로 빼기
		Mqttv5PahoMessageDrivenChannelAdapter messageProducer =
				new Mqttv5PahoMessageDrivenChannelAdapter(mqttConnectionOptions(), "siSampleConsumer", "siSampleTopic");
		messageProducer.setPayloadType(String.class);
//		messageProducer.setConverter(new Mqttv5PahoMessageConverter());
		messageProducer.setManualAcks(true);


		return IntegrationFlow.from(messageProducer)
				.transform(p -> p + ", received from MQTT")
				.handle(logger())
				.get();
	}

	private LoggingHandler logger() {
		LoggingHandler loggingHandler = new LoggingHandler("INFO");
		loggingHandler.setLoggerName("siSample");
		return loggingHandler;
	}

//	@Bean
//	public MessageProducerSupport mqttInbound() {
//		// Adapter 생성
//		Mqttv5PahoMessageDrivenChannelAdapter adapter =
//				new Mqttv5PahoMessageDrivenChannelAdapter(mqttConnectionOptions(),"siSampleConsumer", "siSampleTopic");
//		adapter.setCompletionTimeout(5000);
//		adapter.setMessageConverter(new mqttStringToBytesConverter());
//		adapter.setQos(1);
//		return adapter;
//	}


}
