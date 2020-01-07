/**
 * Copyright 2019 vip.com.
 * <p>
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
 * </p>
 */

package com.vip.pallas.mybatis.entity;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vip.pallas.bean.monitor.MonitorLevelModel;
import org.apache.commons.lang3.StringUtils;

import javax.management.monitor.Monitor;
import java.util.Date;

public class Cluster {

    private Long id;
    
	private String clusterId;

	private String httpAddress;

	private String clientAddress;

	private String description;

	private boolean hasPrivilege = false;    
	/**
     * 当为逻辑集群时，此项不为空，值为集群id集合，逗号分开，如：3,5
     */
    private String realClusters;
    
	private String accessiblePs;

	//只前端用到此字段
	private boolean logicalCluster = false;

	private Date createTime;
	private Date updateTime;

	private String monitorLevel;

	private MonitorLevelModel monitorLevelModel;

	private String username;

	private String passwd;

	public MonitorLevelModel getMonitorLevelModel() {
		return monitorLevelModel;
	}

	public void setMonitorLevelModel(MonitorLevelModel monitorLevelModel) {
		this.monitorLevelModel = monitorLevelModel;
	}

	public void setMonitorLevelModel() {
		if(StringUtils.isNotEmpty(monitorLevel)) {
			this.monitorLevelModel = JSON.parseObject(monitorLevel, MonitorLevelModel.class);
		} else {
			this.monitorLevelModel = MonitorLevelModel.getDefaultModel();
		}
	}

	public void setLogicalCluster(boolean logicalCluster) {
		this.logicalCluster = logicalCluster;
	}

	public String getMonitorLevel() {
		return monitorLevel;
	}

	public void setMonitorLevel(String monitorLevel) {
		this.monitorLevel = monitorLevel;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public boolean isHasPrivilege() {
		return hasPrivilege;
	}

	public void setHasPrivilege(boolean hasPrivilege) {
		this.hasPrivilege = hasPrivilege;
	}

	public String getClusterId() {
		return clusterId;
	}

	public void setClusterId(String clusterId) {
		this.clusterId = clusterId == null ? null : clusterId.trim();
	}

	public String getHttpAddress() {
		return httpAddress;
	}

	public void setHttpAddress(String httpAddress) {
		this.httpAddress = httpAddress == null ? null : httpAddress.trim();
	}

	public String getClientAddress() {
		return clientAddress;
	}

	public void setClientAddress(String clientAddress) {
		this.clientAddress = clientAddress == null ? null : clientAddress.trim();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description == null ? null : description.trim();
	}

	public String getRealClusters() {
		return realClusters;
	}

	public void setRealClusters(String realClusters) {
		this.realClusters = realClusters;
	}

	@JsonIgnore
	public boolean isLogicalCluster() {
		return StringUtils.isNotBlank(getRealClusters());
	}
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAccessiblePs() {
		return accessiblePs;
	}

	public void setAccessiblePs(String accessiblePs) {
		this.accessiblePs = accessiblePs;
	}


	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	@Override
	public String toString() {
		return "Cluster [id=" + id + ", clusterId=" + clusterId + ", httpAddress=" + httpAddress + ", clientAddress="
				+ clientAddress + ", description=" + description + ", hasPrivilege=" + hasPrivilege + ", realClusters="
				+ realClusters + ", accessiblePs=" + accessiblePs + ", logicalCluster=" + logicalCluster
				+ ", createTime=" + createTime + ", updateTime=" + updateTime + ", monitorLevel=" + monitorLevel + "]";
	}
}