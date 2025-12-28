import pymysql
import pandas as pd
import numpy as np
from datetime import datetime
from collections import defaultdict
import warnings

warnings.filterwarnings('ignore')


class MusicHybridRecommender:
    """完整的音乐混合推荐系统 - 同时推荐歌曲和歌单"""

    def __init__(self, db_config, config=None):
        self.db_config = db_config
        self.config = config or {
            'k_neighbors': 5,
            'similarity_threshold': 0.01,
            'min_common_items': 1,
            'min_interactions': 1,
            'music_recommend_ratio': 0.6,
            'playlist_recommend_ratio': 0.4,
            'default_recommendation_count': 10
        }

        # 数据存储
        self.interactions_df = None
        self.playlists_df = None
        self.songlist_df = None

        # 索引和映射
        self.user_index = {}
        self.music_index = {}
        self.playlist_index = {}

        # 矩阵
        self.user_music_matrix = None
        self.user_playlist_matrix = None

        # 缓存
        self.user_similarity_cache = {}
        self.music_popularity = {}
        self.playlist_popularity = {}

        # 状态
        self.is_loaded = False

    def load_data(self):
        """从数据库加载所有数据"""
        print("=" * 50)
        print("开始加载数据...")
        print("=" * 50)

        conn = pymysql.connect(**self.db_config)

        try:
            # 1. 加载用户交互数据
            print("1. 加载用户音乐交互数据...")
            query_interactions = """
            SELECT 
                interaction_id,
                user_id,
                music_id,
                music_list_id,
                singer_id,
                search_content,
                action_type,
                action_value,
                play_duration,
                is_liked,
                is_collected,
                interaction_time
            FROM user_music_interactions
            WHERE music_id IS NOT NULL
            """

            self.interactions_df = pd.read_sql(query_interactions, conn)
            print(f"  加载到 {len(self.interactions_df)} 条交互记录")
            print(f"  用户数: {self.interactions_df['user_id'].nunique()}")
            print(f"  歌曲数: {self.interactions_df['music_id'].nunique()}")

            # 2. 加载歌单数据
            print("\n2. 加载歌单数据...")
            query_playlists = """
            SELECT 
                list_id as playlist_id,
                list_title as playlist_name,
                list_userid as creator_id,
                list_time as create_time,
                list_details as description,
                list_type as playlist_type,
                list_playnum as play_count
            FROM lists
            """

            self.playlists_df = pd.read_sql(query_playlists, conn)
            print(f"  加载到 {len(self.playlists_df)} 个歌单")

            # 3. 加载歌单-歌曲关系
            print("\n3. 加载歌单-歌曲关系...")
            query_songlist = """
            SELECT 
                songlist_id as playlist_id,
                songlist_songid as music_id
            FROM songlist
            """

            self.songlist_df = pd.read_sql(query_songlist, conn)
            print(f"  加载到 {len(self.songlist_df)} 条歌单-歌曲关系")

            # 4. 显示具体数据
            print("\n" + "=" * 50)
            print("数据详情:")
            print("=" * 50)

            if len(self.interactions_df) > 0:
                print("\n用户交互数据示例:")
                print(self.interactions_df[['user_id', 'music_id', 'music_list_id', 'action_type']].head())

            if len(self.playlists_df) > 0:
                print("\n歌单数据示例:")
                print(self.playlists_df[['playlist_id', 'playlist_name', 'play_count']].head())

            if len(self.songlist_df) > 0:
                print("\n歌单-歌曲关系示例:")
                print(self.songlist_df.head())

            self.is_loaded = True
            return True

        except Exception as e:
            print(f"数据加载失败: {e}")
            import traceback
            traceback.print_exc()
            return False

        finally:
            conn.close()

    def preprocess_data(self):
        """预处理数据"""
        if not self.is_loaded or self.interactions_df is None:
            print("错误: 请先加载数据")
            return False

        print("\n" + "=" * 50)
        print("开始预处理数据...")
        print("=" * 50)

        # 1. 处理交互数据
        print("1. 处理交互数据...")

        # 计算行为权重
        self.interactions_df['behavior_weight'] = self.interactions_df.apply(
            self._calculate_behavior_weight, axis=1
        )

        # 显示权重计算示例
        print(f"  交互权重示例:")
        sample_weights = self.interactions_df[['user_id', 'music_id', 'action_type', 'behavior_weight']].head()
        for _, row in sample_weights.iterrows():
            print(
                f"    用户{row['user_id']} -> 歌曲{row['music_id']} ({row['action_type']}): 权重{row['behavior_weight']:.2f}")

        # 创建索引
        self._create_indices()

        # 2. 构建用户-音乐交互矩阵
        print("\n2. 构建用户-音乐交互矩阵...")
        self._build_user_music_matrix()

        # 3. 构建用户-歌单交互矩阵
        print("\n3. 构建用户-歌单交互矩阵...")
        self._build_user_playlist_matrix()

        # 4. 计算流行度
        print("\n4. 计算流行度...")
        self._calculate_popularity()

        print("\n" + "=" * 50)
        print("数据预处理完成!")
        print("=" * 50)

        return True

    def _calculate_behavior_weight(self, row):
        """计算行为权重"""
        # 基于你的数据，action_type都是'like'
        action_type = str(row.get('action_type', '')).lower()

        # 行为类型权重
        if 'like' in action_type:
            base_weight = 3.0
        elif 'play' in action_type:
            base_weight = 1.0
        elif 'collect' in action_type:
            base_weight = 2.5
        else:
            base_weight = 1.0

        # 点赞和收藏加成
        bonus = 0
        if row.get('is_liked') == 1:
            bonus += 1.5
        if row.get('is_collected') == 1:
            bonus += 1.2

        # 播放时长加成
        if pd.notna(row.get('play_duration')) and row['play_duration'] > 0:
            bonus += min(row['play_duration'] / 180, 1.0)

        # 时间衰减
        time_weight = 1.0
        if pd.notna(row.get('interaction_time')):
            try:
                if isinstance(row['interaction_time'], str):
                    interaction_time = pd.to_datetime(row['interaction_time'])
                else:
                    interaction_time = row['interaction_time']

                days_ago = (datetime.now() - interaction_time).days
                time_decay = 0.95 ** min(days_ago, 30)
                time_weight = time_decay
            except:
                pass

        return (base_weight + bonus) * time_weight

    def _create_indices(self):
        """创建索引"""
        print("  创建索引...")

        # 用户索引
        users = self.interactions_df['user_id'].unique()
        self.user_index = {user: idx for idx, user in enumerate(users)}
        print(f"    用户索引: {len(users)} 个用户")

        # 音乐索引
        musics = self.interactions_df['music_id'].unique()
        self.music_index = {music: idx for idx, music in enumerate(musics)}
        print(f"    音乐索引: {len(musics)} 首歌曲")

        # 歌单索引
        if self.playlists_df is not None and len(self.playlists_df) > 0:
            playlists = self.playlists_df['playlist_id'].unique()
            self.playlist_index = {playlist: idx for idx, playlist in enumerate(playlists)}
            print(f"    歌单索引: {len(playlists)} 个歌单")

    def _build_user_music_matrix(self):
        """构建用户-音乐交互矩阵"""
        n_users = len(self.user_index)
        n_musics = len(self.music_index)

        print(f"    矩阵维度: {n_users} 用户 × {n_musics} 歌曲")

        # 使用字典存储最大值
        matrix_dict = {}

        for _, row in self.interactions_df.iterrows():
            user_idx = self.user_index[row['user_id']]
            music_idx = self.music_index[row['music_id']]
            weight = row['behavior_weight']

            key = (user_idx, music_idx)
            if key not in matrix_dict or weight > matrix_dict[key]:
                matrix_dict[key] = weight

        # 转换为列表格式
        rows, cols, values = [], [], []
        for (user_idx, music_idx), weight in matrix_dict.items():
            rows.append(user_idx)
            cols.append(music_idx)
            values.append(weight)

        # 创建稀疏矩阵
        from scipy.sparse import csr_matrix
        self.user_music_matrix = csr_matrix(
            (values, (rows, cols)),
            shape=(n_users, n_musics),
            dtype=np.float32
        )

        print(f"    非零元素: {self.user_music_matrix.nnz}")

        # 显示矩阵内容
        if n_users <= 10 and n_musics <= 10:
            print("\n    用户-音乐矩阵内容:")
            dense_matrix = self.user_music_matrix.toarray()
            for i in range(n_users):
                user_id = list(self.user_index.keys())[list(self.user_index.values()).index(i)]
                row_str = f"    用户{user_id}: "
                for j in range(n_musics):
                    if dense_matrix[i, j] > 0:
                        music_id = list(self.music_index.keys())[list(self.music_index.values()).index(j)]
                        row_str += f"歌曲{music_id}({dense_matrix[i, j]:.1f}) "
                print(row_str)

    def _build_user_playlist_matrix(self):
        """构建用户-歌单交互矩阵"""
        if len(self.playlist_index) == 0:
            print("    无歌单数据，跳过构建用户-歌单矩阵")
            return

        n_users = len(self.user_index)
        n_playlists = len(self.playlist_index)

        print(f"    矩阵维度: {n_users} 用户 × {n_playlists} 歌单")

        # 从交互数据中提取用户-歌单关系
        matrix_dict = defaultdict(float)

        for _, row in self.interactions_df.iterrows():
            if pd.notna(row.get('music_list_id')):
                user_idx = self.user_index[row['user_id']]
                playlist_id = int(row['music_list_id'])

                if playlist_id in self.playlist_index:
                    playlist_idx = self.playlist_index[playlist_id]
                    weight = row['behavior_weight']

                    key = (user_idx, playlist_idx)
                    matrix_dict[key] += weight  # 累加权重

        if not matrix_dict:
            print("    无用户-歌单交互数据")
            return

        # 转换为稀疏矩阵
        rows, cols, values = [], [], []
        for (user_idx, playlist_idx), weight in matrix_dict.items():
            rows.append(user_idx)
            cols.append(playlist_idx)
            values.append(weight)

        from scipy.sparse import csr_matrix
        self.user_playlist_matrix = csr_matrix(
            (values, (rows, cols)),
            shape=(n_users, n_playlists),
            dtype=np.float32
        )

        print(f"    非零元素: {self.user_playlist_matrix.nnz}")

    def _calculate_popularity(self):
        """计算流行度"""
        # 音乐流行度（被交互次数）
        if self.user_music_matrix is not None:
            self.music_popularity = {}
            popularity_counts = np.array(self.user_music_matrix.getnnz(axis=0)).flatten()

            for music_id, music_idx in self.music_index.items():
                if music_idx < len(popularity_counts):
                    self.music_popularity[music_id] = popularity_counts[music_idx]

            print(f"  音乐流行度计算完成: {len(self.music_popularity)} 首歌曲")

        # 歌单流行度
        if self.playlists_df is not None and len(self.playlists_df) > 0:
            self.playlist_popularity = {}
            for _, row in self.playlists_df.iterrows():
                playlist_id = row['playlist_id']
                play_count = row.get('play_count', 0)
                self.playlist_popularity[playlist_id] = play_count

            print(f"  歌单流行度计算完成: {len(self.playlist_popularity)} 个歌单")

    def _calculate_user_similarity(self, user_id1, user_id2):
        """计算两个用户的相似度"""
        if user_id1 not in self.user_index or user_id2 not in self.user_index:
            return 0.0

        user_idx1 = self.user_index[user_id1]
        user_idx2 = self.user_index[user_id2]

        if self.user_music_matrix is None:
            return 0.0

        # 获取用户的评分向量
        ratings1 = self.user_music_matrix[user_idx1].toarray().flatten()
        ratings2 = self.user_music_matrix[user_idx2].toarray().flatten()

        # 找到共同评分的音乐
        common_mask = (ratings1 > 0) & (ratings2 > 0)

        if np.sum(common_mask) == 0:
            return 0.0

        # 提取共同评分
        common_ratings1 = ratings1[common_mask]
        common_ratings2 = ratings2[common_mask]

        # 计算余弦相似度
        dot_product = np.dot(common_ratings1, common_ratings2)
        norm1 = np.linalg.norm(common_ratings1)
        norm2 = np.linalg.norm(common_ratings2)

        if norm1 == 0 or norm2 == 0:
            return 0.0

        similarity = dot_product / (norm1 * norm2)

        return similarity

    def get_similar_users(self, user_id, k=None):
        """获取相似用户"""
        if k is None:
            k = self.config['k_neighbors']

        if user_id not in self.user_similarity_cache:
            # 计算该用户与其他所有用户的相似度
            all_users = list(self.user_index.keys())
            similarities = []

            for other_id in all_users:
                if other_id == user_id:
                    continue

                sim = self._calculate_user_similarity(user_id, other_id)
                if sim > self.config['similarity_threshold']:
                    similarities.append((other_id, sim))

            # 缓存结果
            similarities.sort(key=lambda x: x[1], reverse=True)
            self.user_similarity_cache[user_id] = similarities[:k]

        return self.user_similarity_cache[user_id]

    def recommend_music_cf(self, user_id, n=10):
        """基于协同过滤的歌曲推荐"""
        if user_id not in self.user_index:
            print(f"用户 {user_id} 不在索引中，使用热门推荐")
            return self._popular_music_recommendations(n)

        user_idx = self.user_index[user_id]

        # 获取用户已听的音乐
        if self.user_music_matrix is None or user_idx >= self.user_music_matrix.shape[0]:
            return self._popular_music_recommendations(n)

        user_ratings = self.user_music_matrix[user_idx].toarray().flatten()
        listened_music_indices = set(np.where(user_ratings > 0)[0])

        # 获取相似用户
        similar_users = self.get_similar_users(user_id)

        if not similar_users:
            print(f"用户 {user_id} 无相似用户，使用热门推荐")
            return self._popular_music_recommendations(n, exclude_music=listened_music_indices)

        print(f"用户 {user_id} 找到 {len(similar_users)} 个相似用户:")
        for other_id, sim in similar_users:
            print(f"  用户{other_id}: 相似度 {sim:.3f}")

        # 基于相似用户预测评分
        candidate_scores = {}

        for music_idx in range(self.user_music_matrix.shape[1]):
            if music_idx in listened_music_indices:
                continue

            # 获取用户平均评分
            user_mean = np.mean(user_ratings[user_ratings > 0]) if np.sum(user_ratings > 0) > 0 else 0

            numerator = 0.0
            denominator = 0.0

            for other_id, similarity in similar_users:
                other_idx = self.user_index.get(other_id)
                if other_idx is None or other_idx >= self.user_music_matrix.shape[0]:
                    continue

                other_rating = self.user_music_matrix[other_idx, music_idx]
                other_ratings = self.user_music_matrix[other_idx].toarray().flatten()
                other_mean = np.mean(other_ratings[other_ratings > 0]) if np.sum(other_ratings > 0) > 0 else 0

                if other_rating > 0 and similarity > 0:
                    deviation = other_rating - other_mean
                    numerator += similarity * deviation
                    denominator += abs(similarity)

            if denominator > 0:
                predicted = user_mean + (numerator / denominator)
                if predicted > 0:
                    # 查找音乐ID
                    music_id = None
                    for mid, idx in self.music_index.items():
                        if idx == music_idx:
                            music_id = mid
                            break

                    if music_id is not None:
                        candidate_scores[music_id] = float(predicted)

        # 如果候选歌曲太少，补充热门歌曲
        if len(candidate_scores) < n:
            print(f"协同过滤推荐不足 ({len(candidate_scores)}/{n})，补充热门推荐")
            exclude_music = set(listened_music_indices)
            for music_id in candidate_scores.keys():
                if music_id in self.music_index:
                    exclude_music.add(self.music_index[music_id])

            popular_recs = self._popular_music_recommendations(
                n - len(candidate_scores),
                exclude_music=exclude_music
            )

            for rec in popular_recs:
                music_id = rec['id']
                candidate_scores[music_id] = rec['score'] * 0.5  # 热门歌曲权重降低

        # 排序
        sorted_recommendations = sorted(
            candidate_scores.items(),
            key=lambda x: x[1],
            reverse=True
        )[:n]

        # 格式化为推荐结果
        recommendations = []
        for music_id, score in sorted_recommendations:
            recommendations.append({
                'type': 'music',
                'id': int(music_id),
                'score': float(score),
                'title': f'歌曲_{music_id}',
                'reason': '基于协同过滤推荐',
                'algorithm': 'user_cf'
            })

        return recommendations

    def recommend_music_enhanced(self, user_id, n=10):
        """增强的歌曲推荐（结合多种策略）"""
        if user_id not in self.user_index:
            return self._popular_music_recommendations(n)

        # 1. 协同过滤推荐
        cf_recommendations = self.recommend_music_cf(user_id, n)

        # 2. 基于歌单的推荐
        playlist_recommendations = self._music_recommendations_by_playlist(user_id, n)

        # 3. 合并推荐结果
        all_recommendations = {}

        # 添加协同过滤推荐
        for rec in cf_recommendations:
            music_id = rec['id']
            all_recommendations[music_id] = {
                'rec': rec,
                'cf_score': rec['score']
            }

        # 添加基于歌单的推荐
        for rec in playlist_recommendations:
            music_id = rec['id']
            if music_id in all_recommendations:
                # 如果已存在，合并分数
                all_recommendations[music_id]['rec']['score'] = (
                        all_recommendations[music_id]['cf_score'] * 0.6 +
                        rec['score'] * 0.4
                )
                all_recommendations[music_id]['rec']['reason'] = '综合推荐（协同过滤+歌单）'
                all_recommendations[music_id]['rec']['algorithm'] = 'hybrid'
            else:
                all_recommendations[music_id] = {
                    'rec': rec,
                    'cf_score': 0
                }

        # 转换为列表并排序
        final_recommendations = []
        for music_id, data in all_recommendations.items():
            rec = data['rec']
            # 重新计算综合分数
            if 'cf_score' in data and data['cf_score'] > 0 and rec['score'] > 0:
                rec['score'] = (data['cf_score'] * 0.6 + rec['score'] * 0.4)

            final_recommendations.append(rec)

        # 排序
        final_recommendations.sort(key=lambda x: x['score'], reverse=True)

        return final_recommendations[:n]

    def _music_recommendations_by_playlist(self, user_id, n):
        """基于用户听过的歌单推荐歌曲"""
        if user_id not in self.user_index or self.songlist_df is None or len(self.songlist_df) == 0:
            return []

        # 获取用户听过的歌单
        user_playlists = set()
        if self.user_playlist_matrix is not None:
            user_idx = self.user_index[user_id]
            if user_idx < self.user_playlist_matrix.shape[0]:
                user_playlist_ratings = self.user_playlist_matrix[user_idx].toarray().flatten()
                playlist_indices = np.where(user_playlist_ratings > 0)[0]

                for playlist_idx in playlist_indices:
                    for playlist_id, idx in self.playlist_index.items():
                        if idx == playlist_idx:
                            user_playlists.add(playlist_id)
                            break

        if not user_playlists:
            return []

        # 获取用户已听的音乐
        listened_music = set()
        if self.user_music_matrix is not None:
            user_idx = self.user_index[user_id]
            if user_idx < self.user_music_matrix.shape[0]:
                user_ratings = self.user_music_matrix[user_idx].toarray().flatten()
                listened_indices = np.where(user_ratings > 0)[0]

                for music_idx in listened_indices:
                    for music_id, idx in self.music_index.items():
                        if idx == music_idx:
                            listened_music.add(music_id)
                            break

        # 从用户听过的歌单中推荐歌曲
        candidate_scores = defaultdict(float)

        for playlist_id in user_playlists:
            # 获取歌单中的歌曲
            playlist_music = self.songlist_df[
                self.songlist_df['playlist_id'] == playlist_id
                ]['music_id'].unique()

            for music_id in playlist_music:
                if music_id in listened_music:
                    continue

                # 计算推荐分数
                # 基于歌单的播放量
                playlist_popularity = self.playlist_popularity.get(playlist_id, 0)
                score = np.log1p(playlist_popularity) * 0.5

                candidate_scores[music_id] += score

        # 格式化为推荐结果
        recommendations = []
        for music_id, score in sorted(candidate_scores.items(), key=lambda x: x[1], reverse=True)[:n]:
            recommendations.append({
                'type': 'music',
                'id': int(music_id),
                'score': float(score),
                'title': f'歌曲_{music_id}',
                'reason': '基于您听过的歌单推荐',
                'algorithm': 'playlist_based'
            })

        return recommendations

    def recommend_playlists_cf(self, user_id, n=10):
        """基于协同过滤的歌单推荐"""
        if len(self.playlist_index) == 0 or self.user_playlist_matrix is None:
            return self._popular_playlist_recommendations(n)

        if user_id not in self.user_index:
            return self._popular_playlist_recommendations(n)

        user_idx = self.user_index[user_id]

        if user_idx >= self.user_playlist_matrix.shape[0]:
            return self._popular_playlist_recommendations(n)

        # 获取用户交互过的歌单
        user_ratings = self.user_playlist_matrix[user_idx].toarray().flatten()
        interacted_playlist_indices = set(np.where(user_ratings > 0)[0])

        # 获取相似用户
        similar_users = self.get_similar_users(user_id)

        if not similar_users:
            return self._popular_playlist_recommendations(n, exclude_playlists=interacted_playlist_indices)

        # 基于相似用户预测评分
        candidate_scores = {}

        for playlist_idx in range(self.user_playlist_matrix.shape[1]):
            if playlist_idx in interacted_playlist_indices:
                continue

            # 获取用户平均评分
            user_mean = np.mean(user_ratings[user_ratings > 0]) if np.sum(user_ratings > 0) > 0 else 0

            numerator = 0.0
            denominator = 0.0

            for other_id, similarity in similar_users:
                other_idx = self.user_index.get(other_id)
                if other_idx is None or other_idx >= self.user_playlist_matrix.shape[0]:
                    continue

                other_rating = self.user_playlist_matrix[other_idx, playlist_idx]
                other_ratings = self.user_playlist_matrix[other_idx].toarray().flatten()
                other_mean = np.mean(other_ratings[other_ratings > 0]) if np.sum(other_ratings > 0) > 0 else 0

                if other_rating > 0 and similarity > 0:
                    deviation = other_rating - other_mean
                    numerator += similarity * deviation
                    denominator += abs(similarity)

            if denominator > 0:
                predicted = user_mean + (numerator / denominator)
                if predicted > 0:
                    # 查找歌单ID
                    playlist_id = None
                    for pid, idx in self.playlist_index.items():
                        if idx == playlist_idx:
                            playlist_id = pid
                            break

                    if playlist_id is not None:
                        candidate_scores[playlist_id] = float(predicted)

        # 如果候选歌单太少，补充热门歌单
        if len(candidate_scores) < n:
            exclude_playlists = set(interacted_playlist_indices)
            for playlist_id in candidate_scores.keys():
                if playlist_id in self.playlist_index:
                    exclude_playlists.add(self.playlist_index[playlist_id])

            popular_recs = self._popular_playlist_recommendations(
                n - len(candidate_scores),
                exclude_playlists=exclude_playlists
            )

            for rec in popular_recs:
                playlist_id = rec['id']
                candidate_scores[playlist_id] = rec['score'] * 0.5

        # 排序
        sorted_recommendations = sorted(
            candidate_scores.items(),
            key=lambda x: x[1],
            reverse=True
        )[:n]

        # 格式化为推荐结果
        recommendations = []
        for playlist_id, score in sorted_recommendations:
            # 获取歌单信息
            playlist_name = f'歌单_{playlist_id}'
            description = ''
            music_count = 0

            if self.playlists_df is not None:
                playlist_info = self.playlists_df[
                    self.playlists_df['playlist_id'] == playlist_id
                    ]
                if len(playlist_info) > 0:
                    playlist_name = playlist_info.iloc[0]['playlist_name']
                    description = playlist_info.iloc[0]['description']

            if self.songlist_df is not None:
                music_count = len(self.songlist_df[
                                      self.songlist_df['playlist_id'] == playlist_id
                                      ])

            recommendations.append({
                'type': 'playlist',
                'id': int(playlist_id),
                'score': float(score),
                'name': playlist_name,
                'description': description,
                'music_count': music_count,
                'reason': '基于协同过滤推荐',
                'algorithm': 'user_cf'
            })

        return recommendations

    def _popular_music_recommendations(self, n, exclude_music=None):
        """热门歌曲推荐"""
        if exclude_music is None:
            exclude_music = set()

        if len(self.music_popularity) == 0:
            return []

        # 过滤掉已排除的音乐
        candidate_music = []
        for music_id, popularity in self.music_popularity.items():
            if music_id in self.music_index:
                music_idx = self.music_index[music_id]
                if music_idx not in exclude_music and popularity > 0:
                    candidate_music.append((music_id, popularity))

        # 按流行度排序
        candidate_music.sort(key=lambda x: x[1], reverse=True)

        recommendations = []
        for music_id, popularity in candidate_music[:n]:
            recommendations.append({
                'type': 'music',
                'id': int(music_id),
                'score': float(popularity),
                'title': f'歌曲_{music_id}',
                'reason': '热门歌曲推荐',
                'algorithm': 'popularity'
            })

        return recommendations

    def _popular_playlist_recommendations(self, n, exclude_playlists=None):
        """热门歌单推荐"""
        if exclude_playlists is None:
            exclude_playlists = set()

        if len(self.playlist_popularity) == 0:
            return []

        # 过滤掉已排除的歌单
        candidate_playlists = []
        for playlist_id, popularity in self.playlist_popularity.items():
            if playlist_id in self.playlist_index:
                playlist_idx = self.playlist_index[playlist_id]
                if playlist_idx not in exclude_playlists and popularity > 0:
                    candidate_playlists.append((playlist_id, popularity))

        # 按流行度排序
        candidate_playlists.sort(key=lambda x: x[1], reverse=True)

        recommendations = []
        for playlist_id, popularity in candidate_playlists[:n]:
            # 获取歌单信息
            playlist_name = f'歌单_{playlist_id}'
            description = ''
            music_count = 0

            if self.playlists_df is not None:
                playlist_info = self.playlists_df[
                    self.playlists_df['playlist_id'] == playlist_id
                    ]
                if len(playlist_info) > 0:
                    playlist_name = playlist_info.iloc[0]['playlist_name']
                    description = playlist_info.iloc[0]['description']

            if self.songlist_df is not None:
                music_count = len(self.songlist_df[
                                      self.songlist_df['playlist_id'] == playlist_id
                                      ])

            recommendations.append({
                'type': 'playlist',
                'id': int(playlist_id),
                'score': float(popularity),
                'name': playlist_name,
                'description': description,
                'music_count': music_count,
                'reason': '热门歌单推荐',
                'algorithm': 'popularity'
            })

        return recommendations

    def hybrid_recommend(self, user_id, n=10):
        """混合推荐（歌曲+歌单）"""
        print("\n" + "=" * 50)
        print(f"为用户 {user_id} 生成混合推荐:")
        print("=" * 50)

        # 计算推荐数量
        music_ratio = self.config.get('music_recommend_ratio', 0.6)
        playlist_ratio = self.config.get('playlist_recommend_ratio', 0.4)

        n_music = int(n * music_ratio)
        n_playlist = n - n_music

        print(f"推荐配置: {n_music} 首歌曲 + {n_playlist} 个歌单")

        # 1. 推荐歌曲
        print(f"\n1. 歌曲推荐 ({n_music} 首):")
        music_recommendations = self.recommend_music_enhanced(user_id, n_music)

        if music_recommendations:
            for i, rec in enumerate(music_recommendations, 1):
                print(f"  {i}. 歌曲{rec['id']} (算法: {rec.get('algorithm', 'unknown')}, 得分: {rec['score']:.2f})")
        else:
            print("  无歌曲推荐")

        # 2. 推荐歌单
        print(f"\n2. 歌单推荐 ({n_playlist} 个):")
        playlist_recommendations = self.recommend_playlists_cf(user_id, n_playlist)

        if playlist_recommendations:
            for i, rec in enumerate(playlist_recommendations, 1):
                print(
                    f"  {i}. 歌单{rec['id']}: {rec['name']} (包含{rec['music_count']}首歌曲, 得分: {rec['score']:.2f})")
        else:
            print("  无歌单推荐")

        # 3. 合并推荐结果
        all_recommendations = []

        # 交替插入推荐结果
        max_len = max(len(music_recommendations), len(playlist_recommendations))
        for i in range(max_len):
            if i < len(music_recommendations):
                all_recommendations.append(music_recommendations[i])
            if i < len(playlist_recommendations):
                all_recommendations.append(playlist_recommendations[i])

        return all_recommendations[:n]

    def get_user_info(self, user_id):
        """获取用户信息"""
        info = {
            'user_id': user_id,
            'is_in_index': user_id in self.user_index,
            'music_interactions': 0,
            'playlist_interactions': 0,
            'listened_music': [],
            'interacted_playlists': []
        }

        if user_id in self.user_index:
            user_idx = self.user_index[user_id]

            # 获取听过的音乐
            if self.user_music_matrix is not None and user_idx < self.user_music_matrix.shape[0]:
                user_ratings = self.user_music_matrix[user_idx].toarray().flatten()
                listened_indices = np.where(user_ratings > 0)[0]

                for music_idx in listened_indices:
                    for music_id, idx in self.music_index.items():
                        if idx == music_idx:
                            info['listened_music'].append(music_id)
                            break

                info['music_interactions'] = len(listened_indices)

            # 获取交互过的歌单
            if (self.user_playlist_matrix is not None and
                    user_idx < self.user_playlist_matrix.shape[0]):
                user_ratings = self.user_playlist_matrix[user_idx].toarray().flatten()
                playlist_indices = np.where(user_ratings > 0)[0]

                for playlist_idx in playlist_indices:
                    for playlist_id, idx in self.playlist_index.items():
                        if idx == playlist_idx:
                            info['interacted_playlists'].append(playlist_id)
                            break

                info['playlist_interactions'] = len(playlist_indices)

        return info

    def evaluate(self):
        """简单的模型评估"""
        print("\n" + "=" * 50)
        print("开始模型评估...")
        print("=" * 50)

        if not self.is_loaded:
            print("错误: 请先加载数据")
            return

        metrics = {
            'total_users': len(self.user_index),
            'total_music': len(self.music_index),
            'total_playlists': len(self.playlist_index),
            'total_interactions': len(self.interactions_df) if self.interactions_df is not None else 0,
            'matrix_density': 0,
            'avg_user_interactions': 0
        }

        # 计算矩阵密度
        if self.user_music_matrix is not None:
            n_users, n_music = self.user_music_matrix.shape
            total_cells = n_users * n_music
            if total_cells > 0:
                metrics['matrix_density'] = self.user_music_matrix.nnz / total_cells

        # 计算平均用户交互数
        if self.interactions_df is not None and len(self.user_index) > 0:
            user_counts = self.interactions_df['user_id'].value_counts()
            metrics['avg_user_interactions'] = user_counts.mean() if len(user_counts) > 0 else 0

        # 显示评估结果
        print("\n模型评估结果:")
        print("-" * 30)
        for key, value in metrics.items():
            if isinstance(value, float):
                print(f"{key}: {value:.4f}")
            else:
                print(f"{key}: {value}")

        # 数据稀疏性分析
        print("\n数据稀疏性分析:")
        print("-" * 30)
        density = metrics['matrix_density']
        if density < 0.001:
            print(f"警告: 数据非常稀疏 (密度: {density:.6f})")
            print("建议: 添加更多用户交互数据以提高推荐质量")
        elif density < 0.01:
            print(f"注意: 数据比较稀疏 (密度: {density:.6f})")
            print("建议: 可以考虑使用基于内容的推荐作为补充")
        else:
            print(f"数据密度正常 (密度: {density:.6f})")

        return metrics