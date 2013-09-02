/*
 *  Copyright (C) 2007 - 2013 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.httpproxy.service;

import java.util.Random;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Proxy service test
 * 
 * @author <a href="mailto:aledt84@gmail.com">Alejandro Diaz Torres</a>
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:http-proxy-commons-alternative.xml")
public class ProxyServiceCommonsTest extends BaseProxyServiceTest {

	/**
	 * Proxy properties autowired to be changed
	 */
	@Autowired @Qualifier("runtimeExtProxyProperties")
	private PropertiesConfiguration proxyProperties;

	private static final String timeoutProperty = "timeout";

	/**
	 * Test IProxyService execute as HTTP GET
	 */
	@Test
	public void testExecuteGet() {
		super.testExecuteGet();
	}

	/**
	 * Test IProxyService to fix #6 issue
	 */
	@Test
	public void testChangeProxyConfigurationAtRuntime() {
		try {
			String firstValue = ((Integer) proxy.getProxyConfig().getConnectionTimeout()).toString();
			changeProperties();
			String newValue = (String) ((Integer) proxy.getProxyConfig().getConnectionTimeout()).toString();
			assertNotNull(firstValue);
			assertNotNull(newValue);
			if (firstValue.equals(newValue)) {
				fail("Property ['" + timeoutProperty + "'] hasn't changed!");
			}

			LOGGER.info("Success proxy change properties in runtime;");
			LOGGER.info("Property ['" + timeoutProperty
					+ "'] has changed from '" + firstValue + "' to '"
					+ newValue + "'");

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception executing proxy");
		}
	}

	/**
	 * Change the {@link #testPropertyChange} to a random value and wait 5001 ms
	 * for PropertiesConfiguration class can read the change
	 * 
	 * @throws ConfigurationException
	 * @throws InterruptedException
	 * 
	 * @see org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
	 */
	private void changeProperties() throws ConfigurationException,
			InterruptedException {
		PropertiesConfiguration changeProperty = new PropertiesConfiguration(
				proxyProperties.getFileName());
		changeProperty.load();
		String newValueSetted = new String("" + new Random().nextInt());
		changeProperty.setProperty(timeoutProperty, newValueSetted);
		changeProperty.save();
		// Wait 5001 ms for PropertiesConfiguration class can read the change
		Thread.sleep(5001);
	}

}
