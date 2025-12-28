import numpy as np
from scipy.sparse import csr_matrix, lil_matrix
import warnings

warnings.filterwarnings('ignore')


class UserBasedCFRecommender:
    """基于用户的协同过滤推荐器"""

    def __init__(self, config=None):
        self.config = config or {
            'k_neighbors': 20,
            'similarity_threshold': 0.1,
            'min_common_items': 3,
            'singer_weight': 0.2,
            'playlist_weight': 0.15,
            'enable_hybrid': True
        }

        # 模型数据
        self.user_music_matrix = None
        self.user_singer_matrix = None
        self.user_playlist_matrix = None
        self.user_mean_ratings = None

        # 相似度矩阵
        self.user_similarity = None
        self.user_singer_similarity = None
        self.user_playlist_similarity = None
        self.combined_similarity = None

        # 索引
        self.user_index = {}
        self.music_index = {}
        self.singer_index = {}
        self.playlist_index = {}

        # 辅助数据
        self.music_to_singer = {}
        self.playlist_to_musics = {}

    def fit(self, processed_data):
        """训练模型"""
        print("开始训练协同过滤模型...")

        # 加载数据
        self.user_music_matrix = processed_data['user_music_matrix']
        self.user_singer_matrix = processed_data['user_singer_matrix']
        self.user_playlist_matrix = processed_data['user_playlist_matrix']
        self.user_mean_ratings = processed_data['user_mean_ratings']

        # 加载索引
        indices = processed_data['indices']
        self.user_index = indices['user']
        self.music_index = indices['music']
        self.singer_index = indices.get('singer', {})
        self.playlist_index = indices.get('playlist', {})

        # 加载辅助数据
        processor = processed_data.get('processor')
        if processor:
            self.music_to_singer = getattr(processor, 'music_to_singer', {})
            self.playlist_to_musics = getattr(processor, 'playlist_to_musics', {})

        # 计算相似度
        self._calculate_similarities()

        print("模型训练完成!")
        return self

    def _calculate_similarities(self):
        """计算各种相似度"""
        print("1. 计算用户-音乐相似度...")

        # 确保矩阵是csr格式
        if not isinstance(self.user_music_matrix, csr_matrix):
            self.user_music_matrix = csr_matrix(self.user_music_matrix)

        # 计算余弦相似度
        self.user_similarity = self._safe_cosine_similarity(self.user_music_matrix)

        # 过滤共同评分过少的用户对
        if self.config.get('min_common_items', 0) > 0:
            self._filter_by_common_items()

        print("2. 计算用户-歌手相似度...")
        if self.user_singer_matrix is not None and self.user_singer_matrix.nnz > 0:
            if not isinstance(self.user_singer_matrix, csr_matrix):
                self.user_singer_matrix = csr_matrix(self.user_singer_matrix)
            self.user_singer_similarity = self._safe_cosine_similarity(self.user_singer_matrix)

        print("3. 计算用户-歌单相似度...")
        if self.user_playlist_matrix is not None and self.user_playlist_matrix.nnz > 0:
            if not isinstance(self.user_playlist_matrix, csr_matrix):
                self.user_playlist_matrix = csr_matrix(self.user_playlist_matrix)
            self.user_playlist_similarity = self._safe_cosine_similarity(self.user_playlist_matrix)

        print("4. 组合相似度矩阵...")
        self._combine_similarities()

        print(f"用户相似度矩阵非零元素: {self.combined_similarity.nnz}")

    def _safe_cosine_similarity(self, matrix):
        """安全的余弦相似度计算"""
        if matrix is None or matrix.shape[0] == 0 or matrix.shape[1] == 0:
            return None

        # 方法1: 使用点积计算余弦相似度
        try:
            # 计算每行的L2范数
            norms = np.sqrt(matrix.multiply(matrix).sum(axis=1))
            norms = np.array(norms).flatten()

            # 避免除零错误
            norms[norms == 0] = 1e-10

            # 归一化矩阵
            n_rows = matrix.shape[0]
            norm_diag = csr_matrix((1 / norms, (range(n_rows), range(n_rows))),
                                   shape=(n_rows, n_rows))
            normalized = norm_diag.dot(matrix)

            # 计算相似度
            similarity = normalized.dot(normalized.T)

            # 应用阈值
            threshold = self.config.get('similarity_threshold', 0.1)
            similarity.data[similarity.data < threshold] = 0
            similarity.eliminate_zeros()

            return similarity

        except Exception as e:
            print(f"计算余弦相似度时出错: {e}")
            # 回退到简单的方法
            return self._simple_cosine_similarity(matrix)

    def _simple_cosine_similarity(self, matrix):
        """简单的余弦相似度计算（适用于小规模数据）"""
        if matrix is None:
            return None

        n_users = matrix.shape[0]

        # 转换为密集矩阵（如果数据量不大）
        if n_users < 10000:  # 控制内存使用
            dense_matrix = matrix.toarray()

            # 计算范数
            norms = np.linalg.norm(dense_matrix, axis=1)
            norms[norms == 0] = 1e-10

            # 归一化
            normalized = dense_matrix / norms[:, np.newaxis]

            # 计算相似度
            similarity = np.dot(normalized, normalized.T)

            # 应用阈值
            threshold = self.config.get('similarity_threshold', 0.1)
            similarity[similarity < threshold] = 0

            return csr_matrix(similarity)
        else:
            # 对于大数据，使用近似方法
            return self._approximate_cosine_similarity(matrix)

    def _approximate_cosine_similarity(self, matrix):
        """近似余弦相似度计算（适用于大数据）"""
        n_users = matrix.shape[0]
        similarity = lil_matrix((n_users, n_users), dtype=np.float32)
        threshold = self.config.get('similarity_threshold', 0.1)

        # 计算每行的范数
        norms = np.sqrt(matrix.multiply(matrix).sum(axis=1))
        norms = np.array(norms).flatten()
        norms[norms == 0] = 1e-10

        # 分批计算
        batch_size = 1000

        for i in range(0, n_users, batch_size):
            i_end = min(i + batch_size, n_users)

            for j in range(i, n_users, batch_size):
                j_end = min(j + batch_size, n_users)

                # 计算子矩阵的相似度
                sub_matrix_i = matrix[i:i_end, :]
                sub_matrix_j = matrix[j:j_end, :]

                # 点积
                dot_product = sub_matrix_i.dot(sub_matrix_j.T)

                # 归一化
                for ii in range(i, i_end):
                    idx_i = ii - i
                    norm_i = norms[ii]

                    for jj in range(j, j_end):
                        idx_j = jj - j
                        norm_j = norms[jj]

                        if norm_i > 0 and norm_j > 0:
                            sim = dot_product[idx_i, idx_j] / (norm_i * norm_j)
                            if sim > threshold:
                                similarity[ii, jj] = sim
                                if ii != jj:
                                    similarity[jj, ii] = sim

        return similarity.tocsr()

    def _filter_by_common_items(self):
        """根据共同评分项数量过滤相似度"""
        min_common = self.config.get('min_common_items', 3)

        if self.user_similarity is None or self.user_similarity.nnz == 0:
            return

        # 创建二进制矩阵
        binary_matrix = (self.user_music_matrix > 0).astype(float)

        # 计算共同评分矩阵
        cooccurrence = binary_matrix.dot(binary_matrix.T)

        # 过滤相似度矩阵
        cooccurrence = cooccurrence.tocoo()

        # 只保留共同评分足够的对
        for i, j, count in zip(cooccurrence.row, cooccurrence.col, cooccurrence.data):
            if count < min_common:
                self.user_similarity[i, j] = 0

        self.user_similarity.eliminate_zeros()

    def _combine_similarities(self):
        """组合多种相似度"""
        n_users = len(self.user_index)

        if self.user_similarity is None:
            print("警告: 用户相似度矩阵为空")
            self.combined_similarity = csr_matrix((n_users, n_users))
            return

        # 确保所有相似度矩阵维度一致
        if self.user_similarity.shape[0] != n_users:
            print(f"警告: 用户相似度矩阵维度不匹配 {self.user_similarity.shape[0]} != {n_users}")
            # 调整矩阵大小
            target_shape = (n_users, n_users)
            if self.user_similarity.shape[0] < n_users:
                # 扩展矩阵
                from scipy.sparse import vstack, hstack
                new_matrix = lil_matrix(target_shape)
                new_matrix[:self.user_similarity.shape[0], :self.user_similarity.shape[1]] = self.user_similarity
                self.user_similarity = new_matrix.tocsr()
            else:
                # 裁剪矩阵
                self.user_similarity = self.user_similarity[:n_users, :n_users]

        self.combined_similarity = lil_matrix((n_users, n_users), dtype=np.float32)

        singer_weight = self.config.get('singer_weight', 0.2)
        playlist_weight = self.config.get('playlist_weight', 0.15)
        music_weight = 1.0 - singer_weight - playlist_weight

        # 获取相似用户对
        similarity_pairs = self.user_similarity.tocoo()

        for i, j, sim_music in zip(similarity_pairs.row, similarity_pairs.col, similarity_pairs.data):
            if i >= j:  # 只处理上三角
                continue

            sim_score = 0.0
            weight_sum = 0.0

            # 音乐相似度
            if sim_music > 0:
                sim_score += sim_music * music_weight
                weight_sum += music_weight

            # 歌手相似度
            if self.user_singer_similarity is not None:
                if i < self.user_singer_similarity.shape[0] and j < self.user_singer_similarity.shape[0]:
                    sim_singer = self.user_singer_similarity[i, j]
                    if sim_singer > 0:
                        sim_score += sim_singer * singer_weight
                        weight_sum += singer_weight

            # 歌单相似度
            if self.user_playlist_similarity is not None:
                if i < self.user_playlist_similarity.shape[0] and j < self.user_playlist_similarity.shape[0]:
                    sim_playlist = self.user_playlist_similarity[i, j]
                    if sim_playlist > 0:
                        sim_score += sim_playlist * playlist_weight
                        weight_sum += playlist_weight

            if weight_sum > 0:
                final_sim = sim_score / weight_sum
                threshold = self.config.get('similarity_threshold', 0.1)
                if final_sim > threshold:
                    self.combined_similarity[i, j] = final_sim
                    self.combined_similarity[j, i] = final_sim

        self.combined_similarity = self.combined_similarity.tocsr()

    def find_neighbors(self, user_id, k=20):
        """寻找相似用户"""
        if user_id not in self.user_index:
            return []

        user_idx = self.user_index[user_id]

        if self.combined_similarity is None or self.combined_similarity.shape[0] == 0:
            return []

        # 确保索引在范围内
        if user_idx >= self.combined_similarity.shape[0]:
            return []

        # 获取相似度
        similarities = self.combined_similarity[user_idx].toarray().flatten()
        similarities[user_idx] = -1  # 排除自己

        # 找到top K相似用户
        positive_indices = np.where(similarities > 0)[0]
        if len(positive_indices) == 0:
            return []

        k = min(k, len(positive_indices))
        top_indices = positive_indices[
            np.argsort(similarities[positive_indices])[-k:][::-1]
        ]

        # 映射回用户ID
        idx_to_user = {v: k for k, v in self.user_index.items()}
        neighbors = []
        for idx in top_indices:
            if idx in idx_to_user:
                neighbors.append((idx_to_user[idx], similarities[idx]))

        return neighbors

    def predict(self, user_id, music_id, neighbors=None):
        """预测用户对音乐的评分"""
        if user_id not in self.user_index or music_id not in self.music_index:
            return 0.0

        user_idx = self.user_index[user_id]
        music_idx = self.music_index[music_id]

        # 如果用户已经听过，返回实际评分
        existing_rating = self.user_music_matrix[user_idx, music_idx]
        if existing_rating > 0:
            return float(existing_rating)

        # 获取邻居
        if neighbors is None:
            neighbors = self.find_neighbors(user_id, k=self.config['k_neighbors'])

        if not neighbors:
            return float(self.user_mean_ratings[user_idx] if user_idx < len(self.user_mean_ratings) else 0.0)

        # 基于邻居预测
        user_mean = self.user_mean_ratings[user_idx] if user_idx < len(self.user_mean_ratings) else 0.0

        numerator = 0.0
        denominator = 0.0

        for neighbor_id, similarity in neighbors:
            neighbor_idx = self.user_index.get(neighbor_id)
            if neighbor_idx is None or neighbor_idx >= self.user_music_matrix.shape[0]:
                continue

            neighbor_rating = self.user_music_matrix[neighbor_idx, music_idx]
            neighbor_mean = self.user_mean_ratings[neighbor_idx] if neighbor_idx < len(self.user_mean_ratings) else 0.0

            if neighbor_rating > 0 and similarity > 0:
                deviation = neighbor_rating - neighbor_mean
                numerator += similarity * deviation
                denominator += abs(similarity)

        if denominator == 0:
            return float(user_mean)

        predicted = user_mean + (numerator / denominator)
        return float(max(min(predicted, 5.0), 0.0))

    def recommend(self, user_id, n=10, strategy='hybrid'):
        """
        为用户生成推荐

        Parameters:
        -----------
        user_id : int
            用户ID
        n : int
            推荐数量
        strategy : str
            推荐策略: 'cf_only', 'singer_enhanced', 'hybrid'
        """
        if user_id not in self.user_index:
            return self._cold_start_recommendations(n)

        user_idx = self.user_index[user_id]

        # 获取用户已听的音乐
        if user_idx >= self.user_music_matrix.shape[0]:
            return self._cold_start_recommendations(n)

        user_ratings = self.user_music_matrix[user_idx].toarray().flatten()
        listened_indices = set(np.where(user_ratings > 0)[0])

        # 获取邻居
        neighbors = self.find_neighbors(user_id, k=self.config['k_neighbors'])

        # 预测评分
        candidate_scores = {}
        n_musics = self.user_music_matrix.shape[1]

        for music_idx in range(n_musics):
            if music_idx in listened_indices:
                continue

            # 基础协同过滤预测
            cf_score = self.predict(user_id, music_idx, neighbors)

            if cf_score > 0:
                # 查找音乐ID
                music_id = None
                for mid, idx in self.music_index.items():
                    if idx == music_idx:
                        music_id = mid
                        break

                if music_id is not None:
                    candidate_scores[music_id] = {
                        'cf_score': cf_score,
                        'singer_score': 0.0,
                        'playlist_score': 0.0
                    }

        # 根据策略增强
        if (strategy == 'singer_enhanced' or strategy == 'hybrid') and self.music_to_singer:
            candidate_scores = self._enhance_by_singer_preference(
                user_idx, candidate_scores, listened_indices
            )

        if strategy == 'hybrid' and self.playlist_to_musics:
            candidate_scores = self._enhance_by_playlist_context(
                user_idx, candidate_scores, listened_indices
            )

        # 计算最终得分
        final_scores = []

        cf_weight = 0.6
        singer_weight = 0.25 if strategy in ['singer_enhanced', 'hybrid'] else 0
        playlist_weight = 0.15 if strategy == 'hybrid' else 0

        for music_id, scores in candidate_scores.items():
            total_score = (
                    scores['cf_score'] * cf_weight +
                    scores['singer_score'] * singer_weight +
                    scores['playlist_score'] * playlist_weight
            )

            final_scores.append((music_id, total_score))

        # 排序
        final_scores.sort(key=lambda x: x[1], reverse=True)

        return final_scores[:n]

    def _enhance_by_singer_preference(self, user_idx, candidate_scores, listened_indices):
        """基于歌手偏好增强"""
        if (self.user_singer_matrix is None or
                len(self.singer_index) == 0 or
                len(self.music_to_singer) == 0 or
                user_idx >= self.user_singer_matrix.shape[0]):
            return candidate_scores

        # 获取用户对歌手的偏好
        user_singer_prefs = self.user_singer_matrix[user_idx].toarray().flatten()
        max_singer_pref = np.max(user_singer_prefs) if np.max(user_singer_prefs) > 0 else 1.0

        idx_to_singer = {v: k for k, v in self.singer_index.items()}

        for music_id, scores in candidate_scores.items():
            # 获取这首歌的歌手
            singer_ids = self.music_to_singer.get(music_id, [])
            if not singer_ids:
                continue

            # 计算歌手偏好得分
            singer_score_sum = 0.0
            valid_singers = 0

            for singer_id in singer_ids:
                if singer_id in self.singer_index:
                    singer_idx = self.singer_index[singer_id]
                    if singer_idx < len(user_singer_prefs):
                        pref = user_singer_prefs[singer_idx]
                        if pref > 0:
                            singer_score_sum += pref
                            valid_singers += 1

            if valid_singers > 0:
                avg_score = singer_score_sum / valid_singers
                normalized_score = avg_score / max_singer_pref
                scores['singer_score'] = min(normalized_score, 1.0)

        return candidate_scores

    def _enhance_by_playlist_context(self, user_idx, candidate_scores, listened_indices):
        """基于歌单上下文增强"""
        if (self.user_playlist_matrix is None or
                len(self.playlist_index) == 0 or
                len(self.playlist_to_musics) == 0 or
                user_idx >= self.user_playlist_matrix.shape[0]):
            return candidate_scores

        # 获取用户最常听的歌单
        user_playlist_prefs = self.user_playlist_matrix[user_idx].toarray().flatten()
        if len(user_playlist_prefs) == 0:
            return candidate_scores

        top_playlist_indices = np.argsort(user_playlist_prefs)[-3:][::-1]

        idx_to_playlist = {v: k for k, v in self.playlist_index.items()}

        for music_id, scores in candidate_scores.items():
            # 检查是否在用户常听的歌单中
            in_favorite_playlist = False

            for playlist_idx in top_playlist_indices:
                if playlist_idx < len(user_playlist_prefs) and user_playlist_prefs[playlist_idx] > 0:
                    playlist_id = idx_to_playlist[playlist_idx]
                    playlist_musics = self.playlist_to_musics.get(playlist_id, [])

                    if music_id in playlist_musics:
                        in_favorite_playlist = True
                        break

            if in_favorite_playlist:
                scores['playlist_score'] = 0.7

        return candidate_scores

    def _cold_start_recommendations(self, n):
        """冷启动推荐"""
        if self.user_music_matrix is None or len(self.music_index) == 0:
            return []

        # 基于流行度推荐
        try:
            popularity = np.array(self.user_music_matrix.getnnz(axis=0)).flatten()
            if len(popularity) == 0:
                return []

            popular_indices = np.argsort(popularity)[-n * 2:][::-1]

            idx_to_music = {v: k for k, v in self.music_index.items()}
            recommendations = []

            for idx in popular_indices[:n]:
                if popularity[idx] > 0 and idx in idx_to_music:
                    music_id = idx_to_music[idx]
                    recommendations.append((music_id, float(popularity[idx])))

            return recommendations
        except:
            return []

    def save_model(self, filepath):
        """保存模型"""
        import pickle

        model_data = {
            'config': self.config,
            'user_music_matrix': self.user_music_matrix,
            'user_singer_matrix': self.user_singer_matrix,
            'user_playlist_matrix': self.user_playlist_matrix,
            'user_mean_ratings': self.user_mean_ratings,
            'user_similarity': self.user_similarity,
            'user_singer_similarity': self.user_singer_similarity,
            'user_playlist_similarity': self.user_playlist_similarity,
            'combined_similarity': self.combined_similarity,
            'user_index': self.user_index,
            'music_index': self.music_index,
            'singer_index': self.singer_index,
            'playlist_index': self.playlist_index,
            'music_to_singer': self.music_to_singer,
            'playlist_to_musics': self.playlist_to_musics
        }

        with open(filepath, 'wb') as f:
            pickle.dump(model_data, f)

        print(f"模型保存到: {filepath}")