/**
 *     Copyright (C) 2019-2024 Ubiqube.
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
package com.ubiqube.etsi.mano.vim.k8sexecutor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ubiqube.etsi.mano.vim.k8s.event.ClusterCreateEventHandler;
import com.ubiqube.etsi.mano.vim.k8s.event.ClusterDeleteEventHandler;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

@Service
public class K8sExecutorFabic8Impl implements K8sExecutor {
	/** Logger. */
	private static final Logger LOG = LoggerFactory.getLogger(K8sExecutorFabic8Impl.class);

	@Override
	public <R extends HasMetadata> R create(final Config k8sCfg, final Function<KubernetesClient, R> func) {
		try (KubernetesClient client = new KubernetesClientBuilder().withConfig(k8sCfg).build()) {
			final R res = func.apply(client);
			LOG.info("Created: {}", res.getMetadata().getUid());
		} catch (final KubernetesClientException e) {
			LOG.warn("Error code: {}", e.getCode(), e);
		}
		return null;
	}

	@Override
	public List<StatusDetails> delete(final Config k8sConfig, final Function<KubernetesClient, List<StatusDetails>> func) {
		try (KubernetesClient client = new KubernetesClientBuilder().withConfig(k8sConfig).build()) {
			final List<StatusDetails> res = func.apply(client);
			LOG.info("Deleted: {}", res.size());
			return res;
		} catch (final KubernetesClientException e) {
			LOG.warn("Error code: {}", e.getCode(), e);
		}
		return List.of();
	}

	@Override
	public void waitForClusterDelete(final Config k8sCfg, final HasMetadata obj) {
		try (final KubernetesClient client = new KubernetesClientBuilder().withConfig(k8sCfg).build()) {
			LOG.info("Setting informer");
			final ClusterDeleteEventHandler event = new ClusterDeleteEventHandler();
			try (final SharedIndexInformer<HasMetadata> inf = client.resource(obj).inform((ResourceEventHandler) event)) {
				LOG.info("Cluster delete, Wating for 5 minutes");
				final boolean isTerminatedSuccessfully = event.await(5, TimeUnit.MINUTES);
				if (!isTerminatedSuccessfully) {
					LOG.error("Time out");
				}
			}
		} catch (final InterruptedException e) {
			LOG.warn("Interrupted!", e);
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void waitForClusterCreate(final Config k8sCfg, final HasMetadata obj) {
		try (final KubernetesClient client = new KubernetesClientBuilder().withConfig(k8sCfg).build()) {
			LOG.info("Setting informer");
			final ClusterCreateEventHandler event = new ClusterCreateEventHandler();
			try (final SharedIndexInformer<HasMetadata> inf = client.resource(obj).inform((ResourceEventHandler) event)) {
				LOG.info("Cluster create, Wating for 30 minutes");
				final boolean isTerminatedSuccessfully = event.await(30, TimeUnit.MINUTES);
				if (!isTerminatedSuccessfully) {
					LOG.error("Time out");
				}
			}
		} catch (final InterruptedException e) {
			LOG.warn("Interrupted!", e);
			Thread.currentThread().interrupt();
		}
	}

}
