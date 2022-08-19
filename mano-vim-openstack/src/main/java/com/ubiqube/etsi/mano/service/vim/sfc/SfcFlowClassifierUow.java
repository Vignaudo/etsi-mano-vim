/**
s *     Copyright (C) 2019-2020 Ubiqube.
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
package com.ubiqube.etsi.mano.service.vim.sfc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ubiqube.etsi.mano.dao.mano.nsd.Classifier;
import com.ubiqube.etsi.mano.orchestrator.Context3d;
import com.ubiqube.etsi.mano.orchestrator.NamedDependency;
import com.ubiqube.etsi.mano.orchestrator.NamedDependency2d;
import com.ubiqube.etsi.mano.orchestrator.entities.SystemConnections;
import com.ubiqube.etsi.mano.orchestrator.nodes.mec.VnfExtractorNode;
import com.ubiqube.etsi.mano.orchestrator.nodes.nfvo.VnffgLoadbalancerNode;
import com.ubiqube.etsi.mano.orchestrator.nodes.vnfm.VnfPortNode;
import com.ubiqube.etsi.mano.orchestrator.uow.Relation;
import com.ubiqube.etsi.mano.orchestrator.vt.VirtualTaskV3;
import com.ubiqube.etsi.mano.service.graph.AbstractUnitOfWork;
import com.ubiqube.etsi.mano.service.vim.OsSfc;
import com.ubiqube.etsi.mano.service.vim.sfc.enity.SfcFlowClassifierTask;
import com.ubiqube.etsi.mano.service.vim.sfc.node.FlowClassifierNode;

/**
 *
 * @author Olivier Vignaud <ovi@ubiqube.com>
 *
 */
public class SfcFlowClassifierUow extends AbstractUnitOfWork<SfcFlowClassifierTask> {

	private static final String EXTRACT = "extract-";
	private final SystemConnections vimConnectionInformation;
	private final OsSfc sfc;
	private final SfcFlowClassifierTask task;

	public SfcFlowClassifierUow(final VirtualTaskV3<SfcFlowClassifierTask> task, final SystemConnections vimConnectionInformation) {
		super(task, FlowClassifierNode.class);
		this.vimConnectionInformation = vimConnectionInformation;
		sfc = new OsSfc();
		this.task = task.getTemplateParameters();
	}

	@Override
	public String execute(final Context3d context) {
		final Classifier classifier = task.getClassifier();
		final String src = Optional.ofNullable(classifier.getLogicalSourcePort()).map(x -> context.get(VnfPortNode.class, x)).orElse(null);
		final String dst = Optional.ofNullable(classifier.getLogicalDestinationPort()).map(x -> context.get(VnfPortNode.class, x)).orElse(null);
		return sfc.createFlowClassifier(vimConnectionInformation, classifier, src, dst);
	}

	@Override
	public String rollback(final Context3d context) {
		sfc.deleteFlowClassifier(vimConnectionInformation, task.getVimResourceId());
		return null;
	}

	public List<NamedDependency> getNameDependencies() {
		final List<NamedDependency> ret = new ArrayList<>();
		ret.add(new NamedDependency(VnfExtractorNode.class, EXTRACT + task.getSrcPort()));
		ret.add(new NamedDependency(VnfExtractorNode.class, EXTRACT + task.getDstPort()));
		task.getElement().stream().map(x -> new NamedDependency(VnffgLoadbalancerNode.class, x)).forEach(ret::add);
		return ret;
	}

	public List<NamedDependency> getNamedProduced() {
		return List.of(new NamedDependency(getType(), task.getToscaName()));
	}

	public List<NamedDependency2d> get2dDependencies() {
		final List<NamedDependency2d> ret = new ArrayList<>();
		ret.add(new NamedDependency2d(VnfExtractorNode.class, EXTRACT + task.getSrcPort(), Relation.ONE_TO_ONE));
		ret.add(new NamedDependency2d(VnfExtractorNode.class, EXTRACT + task.getDstPort(), Relation.ONE_TO_ONE));
		task.getElement().stream().map(x -> new NamedDependency2d(VnffgLoadbalancerNode.class, x, Relation.MANY_TO_ONE)).forEach(ret::add);
		return ret;
	}

}
