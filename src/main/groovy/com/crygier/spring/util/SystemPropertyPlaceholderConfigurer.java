/**
 * Copyright 2012 John Crygier
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
package com.crygier.spring.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Property Placeholder Configurer that allows for reading a file that is indicated via a system property.
 * This allows for properties to be loaded from a file that is designated at runtime, and doesn't need to be on the
 * classpath.
 * 
 * To Use, Enter Spring Bean as follows:
 * <pre>
   <bean id="propertyPlaceholderConfigurer" class="com.crygier.spring.util.SystemPropertyPlaceholderConfigurer">
		<property name="systemProperty" value="application.properties.location" />
	 	<property name="ignoreResourceNotFound" value="true" />  
		<property name="locations">  
			<list>  
				<value>classpath:application.properties</value>
			</list>  
		</property>  
	</bean>  
 * </pre>
 * 
 * Then start your application as follows:
 * java -Dapplication.properties.location=/root/test.properties -jar application.jar
 * 
 * This will load properties from application.properties first (from the classpath), then override with /root/test.properties.
 * 
 * @author johnedc
 *
 */
public class SystemPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
	
	private String systemProperty;
	private List<Resource> locations;
	
	public String getSystemProperty() {
		return systemProperty;
	}
	
	public void setSystemProperty(String systemProperty) {
		this.systemProperty = systemProperty;
	}
	
	@Override
	public void setLocations(Resource[] locations) {
		this.locations = new ArrayList<Resource>(Arrays.asList(locations));
		super.setLocations(locations);
	}

	@Override
	protected void loadProperties(Properties props) throws IOException {
		if (getSystemProperty() != null) {
			String systemProp = System.getProperty(getSystemProperty());
			
			if (systemProp != null) {
				File propertiesFile = new File(systemProp);
				
				logger.debug("Adding (" + propertiesFile.getAbsolutePath() + ") to the locations");
				
				if (propertiesFile.exists()) {
					if (locations == null)
						locations = new ArrayList<Resource>();
					
					locations.add(new FileSystemResource(propertiesFile));
					super.setLocations((Resource[]) locations.toArray(new Resource[locations.size()]));
				} else {
					throw new FileNotFoundException(propertiesFile.getAbsolutePath());
				}
			}
		}
			
		super.loadProperties(props);
	}
}
