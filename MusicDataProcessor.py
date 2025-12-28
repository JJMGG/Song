import pymysql
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from scipy.sparse import csr_matrix, lil_matrix
import pickle
from collections import defaultdict
import warnings

warnings.filterwarnings('ignore')


class MusicDataProcessor:
    """音乐数据处理类，针对你的表结构"""

    def __init__(self, db_config, config=None):
        self.db_config = db_config
        self.config = config or {
            'time_decay_factor': 0.95,
            'min_interactions': 5,
            'recent_days': 90
        }

        self.user_index = {}
        self.music_index = {}
        self.singer_index = {}
        self.playlist_index = {}

    def load_data_from_db(self):
        """从数据库加载用户交互数据"""
        print("从数据库加载交互数据...")

        conn = pymysql.connect(**self.db_config)

        # 查询最近N天的数据
        query = f"""
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
        WHERE interaction_time >= DATE_SUB(NOW(), INTERVAL {self.config['recent_days']} DAY)
        AND music_id IS NOT NULL
        ORDER BY interaction_time DESC
        """

        df = pd.read_sql(query, conn)
        conn.close()

        print(f"加载到 {len(df)} 条交互记录")
        print(f"用户数: {df['user_id'].nunique()}")
        print(f"歌曲数: {df['music_id'].nunique()}")

        return df

    def calculate_behavior_weight(self, row):
        """
        根据你的表字段计算行为权重
        你的字段: action_type, action_value, play_duration, is_liked, is_collected
        """
        weights = {}

        # 1. 交互类型权重
        action_type_weights = {
            'play': 1.0,  # 播放
            'like': 3.0,  # 点赞
            'collect': 2.5,  # 收藏
            'search': 0.8,  # 搜索
            'share': 2.0,  # 分享
            'download': 2.2,  # 下载
            'add_to_playlist': 2.8,  # 加入歌单
            'create_playlist': 1.5,  # 创建歌单
            'comment': 1.8,  # 评论
            'skip': -0.5,  # 跳过
            'delete': -1.0  # 删除
        }

        action_type = row.get('action_type', 'play')
        if pd.isna(action_type):
            action_type = 'play'

        # 尝试从action_type中提取基础类型
        base_action = 'play'
        for key in action_type_weights.keys():
            if isinstance(action_type, str) and key in action_type.lower():
                base_action = key
                break

        action_weight = action_type_weights.get(base_action, 1.0)

        # 2. 行为强度权重 (从action_value解析)
        behavior_strength = 1.0
        if pd.notna(row.get('action_value')):
            try:
                # 尝试解析action_value为数值
                if isinstance(row['action_value'], (int, float)):
                    behavior_strength = float(row['action_value'])
                elif isinstance(row['action_value'], str):
                    # 尝试从字符串中提取数字
                    import re
                    numbers = re.findall(r'\d+\.?\d*', row['action_value'])
                    if numbers:
                        behavior_strength = float(numbers[0])
            except:
                behavior_strength = 1.0

        # 限制在合理范围
        behavior_strength = min(max(behavior_strength, 0.1), 5.0)

        # 3. 播放时长权重
        duration_weight = 1.0
        if pd.notna(row.get('play_duration')):
            play_duration = row['play_duration']
            if play_duration > 0:
                # 假设平均歌曲时长180秒
                avg_duration = 150
                completion_ratio = min(play_duration / avg_duration, 1.5)

                if completion_ratio >= 0.9:  # 几乎听完
                    duration_weight = 1.5
                elif completion_ratio >= 0.3:  # 听了一部分
                    duration_weight = 1.0
                else:  # 短时播放
                    duration_weight = 0.5

        # 4. 点赞和收藏权重
        like_weight = 1.0
        if pd.notna(row.get('is_liked')):
            like_weight += 1.5 if row['is_liked'] == 1 else 0

        collect_weight = 1.0
        if pd.notna(row.get('is_collected')):
            collect_weight += 1.2 if row['is_collected'] == 1 else 0

        # 5. 时间衰减权重
        time_weight = 1.0
        if pd.notna(row.get('interaction_time')):
            if isinstance(row['interaction_time'], str):
                try:
                    interaction_time = pd.to_datetime(row['interaction_time'])
                except:
                    interaction_time = datetime.now()
            else:
                interaction_time = row['interaction_time']

            days_ago = (datetime.now() - interaction_time).days
            time_decay = self.config['time_decay_factor']
            time_weight = time_decay ** min(days_ago, 30)

        # 6. 歌单上下文权重
        playlist_weight = 1.0
        if pd.notna(row.get('music_list_id')):
            # 从歌单播放通常表示更强的兴趣
            playlist_weight = 1.3

        # 7. 搜索行为权重
        search_weight = 1.0
        if pd.notna(row.get('search_content')) and row.get('search_content'):
            # 主动搜索表明明确兴趣
            search_weight = 1.4

        # 8. 综合权重计算
        weights_config = {
            'action': {'weight': action_weight, 'importance': 0.25},
            'behavior': {'weight': behavior_strength, 'importance': 0.15},
            'duration': {'weight': duration_weight, 'importance': 0.15},
            'like': {'weight': like_weight, 'importance': 0.10},
            'collect': {'weight': collect_weight, 'importance': 0.10},
            'time': {'weight': time_weight, 'importance': 0.10},
            'playlist': {'weight': playlist_weight, 'importance': 0.10},
            'search': {'weight': search_weight, 'importance': 0.05}
        }

        # 计算加权平均
        total_weight = 0.0
        total_importance = 0.0

        for key, params in weights_config.items():
            weight = params['weight']
            importance = params['importance']

            if weight > 0:  # 只考虑正权重
                total_weight += weight * importance
                total_importance += importance

        composite_weight = total_weight / total_importance if total_importance > 0 else 0

        # 归一化到0-5分制
        normalized_score = min(composite_weight * 1.2, 5.0)

        return normalized_score

    def prepare_interaction_matrix(self, df):
        """准备用户-音乐交互矩阵"""
        print("准备用户-音乐交互矩阵...")

        # 复制数据避免修改原数据
        data = df.copy()

        # 1. 处理缺失值
        data['music_list_id'] = data['music_list_id'].fillna(-1)
        data['singer_id'] = data['singer_id'].fillna(-1)
        data['search_content'] = data['search_content'].fillna('')

        # 2. 计算行为权重
        print("计算行为权重...")
        data['behavior_weight'] = data.apply(
            lambda row: self.calculate_behavior_weight(row),
            axis=1
        )

        # 3. 过滤低活跃用户
        user_interaction_counts = data['user_id'].value_counts()
        active_users = user_interaction_counts[
            user_interaction_counts >= self.config['min_interactions']
            ].index
        data = data[data['user_id'].isin(active_users)]

        print(f"活跃用户数: {len(active_users)}")
        print(f"活跃用户交互数: {len(data)}")

        # 4. 创建索引
        self._create_indices(data)

        # 5. 构建用户-音乐交互矩阵
        print("构建用户-音乐矩阵...")
        user_music_matrix = self._build_interaction_matrix(data)

        # 6. 构建辅助矩阵
        print("构建辅助矩阵...")
        user_singer_matrix = self._build_user_singer_matrix(data)
        user_playlist_matrix = self._build_user_playlist_matrix(data)

        # 7. 计算用户平均评分
        user_mean_ratings = self._calculate_user_means(user_music_matrix)

        return {
            'user_music_matrix': user_music_matrix,
            'user_singer_matrix': user_singer_matrix,
            'user_playlist_matrix': user_playlist_matrix,
            'user_mean_ratings': user_mean_ratings,
            'data': data,
            'indices': {
                'user': self.user_index,
                'music': self.music_index,
                'singer': self.singer_index,
                'playlist': self.playlist_index
            }
        }

    def _create_indices(self, data):
        """创建索引映射"""
        # 用户索引
        users = data['user_id'].unique()
        self.user_index = {user: idx for idx, user in enumerate(users)}

        # 音乐索引
        musics = data['music_id'].unique()
        self.music_index = {music: idx for idx, music in enumerate(musics)}

        # 歌手索引（排除-1）
        singer_data = data[data['singer_id'] != -1]
        singers = singer_data['singer_id'].unique()
        self.singer_index = {singer: idx for idx, singer in enumerate(singers)}

        # 歌单索引（排除-1）
        playlist_data = data[data['music_list_id'] != -1]
        playlists = playlist_data['music_list_id'].unique()
        self.playlist_index = {playlist: idx for idx, playlist in enumerate(playlists)}

        print(f"索引统计 - 用户: {len(users)}, 音乐: {len(musics)}")
        print(f"         - 歌手: {len(singers)}, 歌单: {len(playlists)}")

        # 构建音乐-歌手映射
        self.music_to_singer = {}
        for _, row in singer_data.iterrows():
            music_id = row['music_id']
            singer_id = row['singer_id']

            if music_id not in self.music_to_singer:
                self.music_to_singer[music_id] = []

            if singer_id not in self.music_to_singer[music_id]:
                self.music_to_singer[music_id].append(singer_id)

        # 构建歌单-音乐映射
        self.playlist_to_musics = defaultdict(list)
        for _, row in playlist_data.iterrows():
            playlist_id = row['music_list_id']
            music_id = row['music_id']

            if music_id not in self.playlist_to_musics[playlist_id]:
                self.playlist_to_musics[playlist_id].append(music_id)

    def _build_interaction_matrix(self, data):
        """构建用户-音乐交互矩阵"""
        n_users = len(self.user_index)
        n_musics = len(self.music_index)

        print(f"构建矩阵: {n_users} 用户 x {n_musics} 音乐")

        # 使用csr_matrix直接构建
        rows = []
        cols = []
        values = []

        # 为每个用户记录最大权重
        user_music_dict = {}

        for _, row in data.iterrows():
            user_idx = self.user_index.get(row['user_id'])
            music_idx = self.music_index.get(row['music_id'])
            weight = row['behavior_weight']

            if user_idx is not None and music_idx is not None:
                key = (user_idx, music_idx)
                if key not in user_music_dict or weight > user_music_dict[key]:
                    user_music_dict[key] = weight

        # 转换为稀疏矩阵格式
        for (user_idx, music_idx), weight in user_music_dict.items():
            rows.append(user_idx)
            cols.append(music_idx)
            values.append(weight)

        # 创建稀疏矩阵
        matrix = csr_matrix((values, (rows, cols)), shape=(n_users, n_musics), dtype=np.float32)

        density = matrix.nnz / (n_users * n_musics) if (n_users * n_musics) > 0 else 0
        print(f"矩阵维度: {matrix.shape}, 非零元素: {matrix.nnz}, 稀疏度: {density:.6f}")

        return matrix

    def _build_user_singer_matrix(self, data):
        """构建用户-歌手交互矩阵"""
        singer_data = data[data['singer_id'] != -1]

        if len(self.singer_index) == 0:
            return None

        n_users = len(self.user_index)
        n_singers = len(self.singer_index)

        print(f"构建用户-歌手矩阵: {n_users} 用户 x {n_singers} 歌手")

        # 使用字典累计权重
        user_singer_dict = {}

        for _, row in singer_data.iterrows():
            user_idx = self.user_index.get(row['user_id'])
            singer_id = row['singer_id']

            if user_idx is not None and singer_id in self.singer_index:
                singer_idx = self.singer_index[singer_id]
                weight = row['behavior_weight']

                key = (user_idx, singer_idx)
                if key in user_singer_dict:
                    user_singer_dict[key] += weight
                else:
                    user_singer_dict[key] = weight

        if not user_singer_dict:
            return None

        # 转换为稀疏矩阵
        rows, cols, values = [], [], []
        for (user_idx, singer_idx), weight in user_singer_dict.items():
            rows.append(user_idx)
            cols.append(singer_idx)
            values.append(weight)

        matrix = csr_matrix((values, (rows, cols)), shape=(n_users, n_singers), dtype=np.float32)

        print(f"用户-歌手矩阵: {matrix.shape}, 非零元素: {matrix.nnz}")
        return matrix

    def _build_user_playlist_matrix(self, data):
        """构建用户-歌单交互矩阵"""
        playlist_data = data[data['music_list_id'] != -1]

        if len(self.playlist_index) == 0:
            return None

        n_users = len(self.user_index)
        n_playlists = len(self.playlist_index)

        print(f"构建用户-歌单矩阵: {n_users} 用户 x {n_playlists} 歌单")

        # 使用字典累计权重
        user_playlist_dict = {}

        for _, row in playlist_data.iterrows():
            user_idx = self.user_index.get(row['user_id'])
            playlist_id = row['music_list_id']

            if user_idx is not None and playlist_id in self.playlist_index:
                playlist_idx = self.playlist_index[playlist_id]
                weight = row['behavior_weight']

                key = (user_idx, playlist_idx)
                if key in user_playlist_dict:
                    user_playlist_dict[key] += weight
                else:
                    user_playlist_dict[key] = weight

        if not user_playlist_dict:
            return None

        # 转换为稀疏矩阵
        rows, cols, values = [], [], []
        for (user_idx, playlist_idx), weight in user_playlist_dict.items():
            rows.append(user_idx)
            cols.append(playlist_idx)
            values.append(weight)

        matrix = csr_matrix((values, (rows, cols)), shape=(n_users, n_playlists), dtype=np.float32)

        print(f"用户-歌单矩阵: {matrix.shape}, 非零元素: {matrix.nnz}")
        return matrix

    def _calculate_user_means(self, user_matrix):
        """计算用户平均评分"""
        n_users = user_matrix.shape[0]
        means = np.zeros(n_users, dtype=np.float32)

        for i in range(n_users):
            ratings = user_matrix[i].toarray().flatten()
            nonzero_ratings = ratings[ratings > 0]
            if len(nonzero_ratings) > 0:
                means[i] = np.mean(nonzero_ratings)

        return means