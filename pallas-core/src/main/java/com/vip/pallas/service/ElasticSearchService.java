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

package com.vip.pallas.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.vip.pallas.bean.monitor.ShardInfoModel;
import com.vip.pallas.mybatis.entity.IndexVersion;
import org.elasticsearch.client.RestClient;

import com.vip.pallas.mybatis.entity.Cluster;

public interface ElasticSearchService {
	
	public String genMappingJsonByVersionIdAndClusterName(Long versionId, String clusterName);
	
	public String createIndex(String indexName, Long indexId, Long versionId) throws IOException;

	public String deleteIndex(String indexName, Long indexId, Long versionId) throws IOException;

	public String deleteIndex(String clusterAddress, String fullIndexName, String username, String password);

	public String deleteAliasIndexByCluster(Long indexId, String indexName, Long versionId, Cluster cluster)
			throws Exception;

	String transferAliasIndex(Long indexId, String indexName, Long targetVersionId, Cluster targetCluster) throws Exception;
	
	public boolean isExistIndex(String indexName, String clusterHttpAddress, Long versionId, String username, String password);
	
	public Long getDataCount(String indexName, String httpAddress, Long versionId, String username, String password);
	
	public String getIndexInfo(String indexName, String httpAddress, Long versionId, String username, String passwd);

	boolean excludeOneNode(String host, String nodeIp, String username, String passwd);
	
	boolean includeOneNode(String host, String nodeIp, String username, String passwd);

	List<String> getExcludeNodeList(String clusterAddress, String username, String passwd);

	List<String> getAvalableNodeIps(String clusterAddress, String username, String passwd) throws IOException;

	/**
	 * 根据索引名（别名）找出全部索引分布集群及分片节点
	 * @param aliasIndexNames
	 * @return
	 */
	Map<String, List<String>> getShardNodesByAlias(String aliasIndexNames);

	String getClusterStatus(String clusterName) throws Exception;

	Map<String, String> getMainClusterSettings(String clusterName) throws Exception;

	List<String[]> getNodes(String clusterName) throws Exception;

	List<String[]> getNodesInfos(String clusterName) throws Exception;

	List<String[]> getShards(String clusterName) throws Exception;

	// _cat/indices
	List<String[]> getIndexInfos(String clusterName) throws Exception;

	Map<String/*nodeName*/, ShardInfoModel> getShardsNode(String clusterName) throws Exception;

	Map<String/*IndexName*/, ShardInfoModel> getShardsIndex(String clusterName) throws Exception;

	List<String[]> getActualIndexs(String clusterHttpAddress, String username, String passwd);

	List<String[]> getNormalIndexs(String clusterHttpAddress, String username, String passwd);

	List<String[]> getIndexAndNodes(String clusterHttpAddress, String username, String passwd);

	Cluster getHttpAddressByClusterName(String clusterName);

	RestClient getRestClientByClusterName(String clusterName);

	String setIndexBlockWrite(RestClient clusterRestClient, String actualIndexName, boolean isBlockWrite) throws IOException;

	String setClusterAllocation(RestClient restClient, String enable) throws IOException;

	String setClusterRebalance(RestClient restClient, String enable) throws IOException;

	String setClusterDelayedTimeout(RestClient restClient, String timeout) throws IOException;

	String setClusterFlushSynced(RestClient restClient) throws IOException;

	String executeDeleteByQueryDsl(Long indexId, Long versionId, String indexName, String dsl, int scrollSize);

	String executeSearchByDsl(Long indexId, Long versionId, String indexName, String dsl);

	String cancelDeleteByQueryTask(Long versionId, String indexName);

	public String retrieveIndex(String indexName, String httpAddress, Long versionId,String username, String passwd);

	String runDsl(String httpAddress, String endPoint, String username, String passwd) throws IOException;

	String queryByDsl(String queryString, String endPoint, Cluster cluster) throws IOException;

	Map<Integer, List<String>> loadShardDistributionMap(String indexAliasName, String httpAddress) throws IOException;

	List<HashSet<String>> dynamicDevideShards2Group(String indexAliasName, String httpAddress, String username,
													String passwd) throws IOException;

	String dynamicUpdateIndexSettings(String indexName, Long indexId, IndexVersion indexVersion) throws IOException;
}