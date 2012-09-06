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
package com.crygier.spring.util.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.oxm.xstream.XStreamMarshaller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractCachingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;
import org.springframework.web.servlet.view.xml.MarshallingView;

/**
 * Utilizes the MIME Type from the Accept Header of the request to choose the proper view resolver.  It will
 * allow you to choose your own mappings by setting the 'mimeTypeToViewResolver' property.  The intent here is
 * to allow for extremely simple REST-like interfaces.  A sample Spring Setup would be as follows:
 * 
 * <pre>
   <bean id="tilesViewResolver" class="org.springframework.web.servlet.view.UrlBasedViewResolver"
	      p:viewClass="org.springframework.web.servlet.view.tiles2.TilesView" />
 
 	<bean class="com.crygier.spring.util.web.MimeTypeViewResolver">
 		<property name="mimeTypeToViewResolver">
 			<map>
 				<entry key="text/html" value-ref="tilesViewResolver"/>
 			</map>
 		</property>
 	</bean>
 * </pre>
 * 
 * If you don't set any mappings, there are several defaults:
 * text/html - org.springframework.web.servlet.view.InternalResourceViewResolver
 *           - Default Prefix is /.  Override by setting 'defaultInternalResourcePrefix'
 *           - Default Suffix is .jsp.  Override by setting 'defaultInternalResourceSuffix'
 * application/json - org.springframework.web.servlet.view.json.MappingJacksonJsonView
 * text/xml - org.springframework.oxm.xstream.XStreamMarshaller
 * 
 * This means that Controllers should know nothing about Views, and should use the @RequestMapping annotation in this
 * class to tell it how to resolve views that need a name, just as the JSP view resolver.  So, to map to a JSP view with
 * the default text/html view handler, setting the annotation "@ResponseMapping("welcome")" on a method will resolve to
 * "/welcome.jsp".
 * 
 * @author John Crygier
 *
 */
public class MimeTypeViewResolver extends AbstractCachingViewResolver implements Ordered, InitializingBean
{
	public static final Log logger = LogFactory.getLog(MimeTypeViewResolver.class);
	
	private List<HandlerMapping> handlerMappings;
	
	private Map<String, ViewResolver> mimeTypeToViewResolver = new HashMap<String, ViewResolver>();
	private String defaultInternalResourcePrefix = "/";
	private String defaultInternalResourceSuffix = ".jsp";
	
	private int order = HIGHEST_PRECEDENCE;

	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		String name = getViewNameFromRequest();

		String acceptHeader = getCurrentRequestAttributes().getRequest().getHeader("Accept");
		
		List<MediaType> acceptableMediaTypes = null;
		if (StringUtils.hasText(acceptHeader)) {
			try {
				acceptableMediaTypes = MediaType.parseMediaTypes(acceptHeader);
			} catch (Exception e) {
				logger.warn("Unparsable Accept header, defaulting to text/html");
				acceptableMediaTypes = new ArrayList<MediaType>();
				acceptableMediaTypes.add(MediaType.TEXT_HTML);
			}
		}
		
		for (MediaType requestedMediaType : acceptableMediaTypes) {
			ViewResolver viewResolver = findViewResolverForMediaType(requestedMediaType);
			
			if (viewResolver != null)
				return viewResolver.resolveViewName(name, locale);
		}
		
		logger.warn("No view resolvers found, backing onto Default text/html resolver");
		return mimeTypeToViewResolver.get(MediaType.TEXT_HTML_VALUE).resolveViewName(name, locale);
	}
	
	protected ViewResolver findViewResolverForMediaType(MediaType requestedMediaType) {
		for (String mimeType : mimeTypeToViewResolver.keySet()) {
			MediaType searchingType = MediaType.parseMediaType(mimeType);
			
			if (requestedMediaType.isCompatibleWith(searchingType))
				return mimeTypeToViewResolver.get(mimeType);
		}
		
		return null;
	}
	
	/**
	 * Gets the name of the view to resolve from the request.  First looks to see if there is a ResponseMapping
	 * annotation on the method that is called, and uses the value attribute of that.  If that is not found,
	 * it will use the name of the method that is called.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected String getViewNameFromRequest() throws Exception {
		HandlerExecutionChain execution = getHandler(getCurrentRequestAttributes().getRequest());
		HandlerMethod hm = (HandlerMethod) execution.getHandler();
		String name = hm.getMethod().getName();
		
		if (hm.getMethod().isAnnotationPresent(ResponseMapping.class)) {
			ResponseMapping responseMapping = hm.getMethod().getAnnotation(ResponseMapping.class);
			name = responseMapping.value()[0];
		}
		
		return name;
	}
	
	protected List<HandlerMapping> getHandlerMappings() {
		if (this.handlerMappings == null) {
			Map<String, HandlerMapping> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(getApplicationContext(), HandlerMapping.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
				// We keep HandlerMappings in sorted order.
				OrderComparator.sort(this.handlerMappings);
			}
		}
		
		return this.handlerMappings;
	}
	
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		for (HandlerMapping hm : getHandlerMappings()) {
			HandlerExecutionChain handler = hm.getHandler(request);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}
	
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		String name = null;
		
		try {
			name = getViewNameFromRequest();
		} catch (Exception e) {}
		
		String acceptHeader = getCurrentRequestAttributes().getRequest().getHeader("Accept");
		
		return name + "_" + locale + "_" + acceptHeader;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		// After the properties are set, throw the defaults into the mimeTypeMappings
		
		if (mimeTypeToViewResolver.containsKey(MediaType.TEXT_HTML_VALUE) == false) {
			InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
			viewResolver.setPrefix(defaultInternalResourcePrefix);
			viewResolver.setSuffix(defaultInternalResourceSuffix);
			viewResolver.setApplicationContext(getApplicationContext());
			
			mimeTypeToViewResolver.put(MediaType.TEXT_HTML_VALUE, viewResolver);
		}
		
		if (mimeTypeToViewResolver.containsKey(MediaType.APPLICATION_JSON_VALUE) == false) {
			ViewResolver viewResolver = new ViewResolver() {
				@Override
				public View resolveViewName(String viewName, Locale locale) throws Exception {
					MappingJacksonJsonView jsonView = new MappingJacksonJsonView();
					jsonView.setApplicationContext(getApplicationContext());
					return jsonView;
				}
			};
			
			mimeTypeToViewResolver.put(MediaType.APPLICATION_JSON_VALUE, viewResolver);
		}
		
		if (mimeTypeToViewResolver.containsKey(MediaType.TEXT_XML_VALUE) == false) {
			ViewResolver viewResolver = new ViewResolver() {
				@Override
				public View resolveViewName(String viewName, Locale locale) throws Exception {
					XStreamMarshaller marshaller = new XStreamMarshaller();
					marshaller.setAutodetectAnnotations(true);
					MarshallingView view = new MarshallingView(marshaller);
					
					return view;
				}
			};
			
			mimeTypeToViewResolver.put(MediaType.TEXT_XML_VALUE, viewResolver);
		}
		
	}
	
	protected ServletRequestAttributes getCurrentRequestAttributes() {
		return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
	}
	
	public void setMimeTypeToViewResolver(Map<String, ViewResolver> mimeTypeToViewResolver) {
		this.mimeTypeToViewResolver = mimeTypeToViewResolver;
	}
	
	/**
	 * Sets the default InternalResourceViewResolver Prefix for mapping with the default handler
	 * for text/html.
	 * 
	 * @param defaultInternalResourcePrefix
	 */
	public void setDefaultInternalResourcePrefix(String defaultInternalResourcePrefix) {
		this.defaultInternalResourcePrefix = defaultInternalResourcePrefix;
	}
	
	/**
	 * Sets the default InternalResourceViewResolver Suffix for mapping with the default handler
	 * for text/html.
	 * 
	 * @param defaultInternalResourceSuffix
	 */
	public void setDefaultInternalResourceSuffix(String defaultInternalResourceSuffix) {
		this.defaultInternalResourceSuffix = defaultInternalResourceSuffix;
	}
	
	@Override
	public int getOrder() {
		return order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Mapping
	public @interface ResponseMapping {
		String[] value() default {};
	}
}