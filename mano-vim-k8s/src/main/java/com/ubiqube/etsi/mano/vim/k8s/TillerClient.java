/**
 *     Copyright (C) 2019-2020 Ubiqube.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ubiqube.etsi.mano.vim.k8s;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Future;

import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;

import com.ubiqube.etsi.mano.service.vim.VimException;
import com.ubiqube.etsi.mano.service.vim.k8s.K8sClient;

import hapi.release.ReleaseOuterClass.Release;
import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.InstallReleaseResponse;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

/**
 *
 * @author Olivier Vignaud <ovi@ubiqube.com>
 *
 */
public class TillerClient implements K8sClient {

	private final DefaultKubernetesClient client;
	private final Tiller tiller;
	private final long timeout = 300L;

	private TillerClient(final Config config) {
		this.client = new DefaultKubernetesClient(config);
		try {
			this.tiller = new Tiller(client);
		} catch (final MalformedURLException e) {
			throw new VimException(e);
		}
	}

	private static ConfigBuilder getBaseBuilder(final URL url, final String ca) {
		return new ConfigBuilder()
				.withMasterUrl(url.toString())
				.withCaCertData(ca);
	}

	public static TillerClient ofCerts(final URL url, final String ca, final String clientCert, final String clientKey) {
		final Config c = getBaseBuilder(url, ca)
				.withClientCertData(clientCert)
				.withClientKeyData(clientKey)
				.build();
		return new TillerClient(c);
	}

	public static TillerClient ofToken(final URL url, final String ca, final String username, final String token) {
		final Config c = getBaseBuilder(url, ca)
				.withUsername(username)
				.withOauthToken(token)
				.build();
		return new TillerClient(c);
	}

	@Override
	public String deploy() {
		try (final ReleaseManager releaseManager = new ReleaseManager(tiller)) {
			final InstallReleaseRequest.Builder requestBuilder = InstallReleaseRequest.newBuilder();
			requestBuilder.setTimeout(timeout);
			requestBuilder.setName("test-charts"); // Set the Helm release name
			requestBuilder.setWait(true); // Wait for Pods to be ready
			requestBuilder.setNamespace(UUID.randomUUID().toString());
			final Future<InstallReleaseResponse> releaseFuture = releaseManager.install(requestBuilder, chart);
			assert releaseFuture != null;
			final Release release = releaseFuture.get().getRelease();
			System.out.println("release => " + release.getInfo().getStatus().getCode());
		} catch (final IOException e) {
			throw new VimException(e);
		}
		return null;
	}

}
