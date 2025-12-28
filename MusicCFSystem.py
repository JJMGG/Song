import pickle

import numpy as np

from MusicDataProcessor import MusicDataProcessor
from UserBasedCFRecommender import UserBasedCFRecommender


class MusicCFSystem:
    """完整的音乐协同过滤推荐系统"""

    def __init__(self, db_config, config=None):
        self.db_config = db_config
        self.config = config or {
            'k_neighbors': 20,
            'similarity_threshold': 0.1,
            'min_common_items': 3,
            'singer_weight': 0.2,
            'playlist_weight': 0.15,
            'time_decay_factor': 0.95,
            'min_interactions': 5,
            'recent_days': 90
        }

        self.processor = MusicDataProcessor(db_config, self.config)
        self.recommender = UserBasedCFRecommender(self.config)
        self.is_trained = False

    def train(self):
        """训练推荐系统"""
        print("=" * 50)
        print("开始训练音乐推荐系统")
        print("=" * 50)

        # 1. 加载数据
        df = self.processor.load_data_from_db()

        if len(df) == 0:
            print("错误: 没有加载到数据")
            return False

        # 2. 处理数据
        processed_data = self.processor.prepare_interaction_matrix(df)
        processed_data['processor'] = self.processor

        # 3. 训练模型
        self.recommender.fit(processed_data)

        self.is_trained = True

        print("=" * 50)
        print("推荐系统训练完成!")
        print("=" * 50)

        return True

    def recommend(self, user_id, n=10, strategy='hybrid', enrich=True):
        """生成推荐"""
        if not self.is_trained:
            raise ValueError("请先训练模型")

        # 生成推荐
        recommendations = self.recommender.recommend(user_id, n, strategy)

        # 丰富推荐结果
        if enrich:
            recommendations = self._enrich_recommendations(recommendations)

        return recommendations

    def _enrich_recommendations(self, recommendations):
        """丰富推荐结果"""
        enriched = []

        for music_id, score in recommendations:
            music_info = {
                'music_id': int(music_id),
                'score': float(score),
                'title': self._get_music_title(music_id),
                'singer': self._get_music_singer(music_id),
                'reason': self._generate_reason(music_id, score)
            }
            enriched.append(music_info)

        return enriched

    def _get_music_title(self, music_id):
        """获取歌曲标题（示例，应从数据库获取）"""
        # 这里应该查询数据库
        return f"歌曲_{music_id}"

    def _get_music_singer(self, music_id):
        """获取歌手信息"""
        singer_ids = self.processor.music_to_singer.get(music_id, [])
        if singer_ids:
            return f"歌手_{singer_ids[0]}"
        return "未知歌手"

    def _generate_reason(self, music_id, score):
        """生成推荐理由"""
        reasons = []

        if score >= 4.0:
            reasons.append("高分推荐")
        elif score >= 3.0:
            reasons.append("热门推荐")

        # 检查是否在用户常听的歌手
        singer_ids = self.processor.music_to_singer.get(music_id, [])
        if singer_ids and any(sid in self.recommender.singer_index for sid in singer_ids):
            reasons.append("您常听的歌手")

        if reasons:
            return " | ".join(reasons)

        return "发现好歌"

    def evaluate(self, test_ratio=0.2, n_users=100):
        """评估模型"""
        if not self.is_trained:
            raise ValueError("请先训练模型")

        print("开始模型评估...")

        # 这里需要测试集数据
        # 简化的评估逻辑

        # 可以计算覆盖率、多样性等指标
        metrics = {
            'coverage': self._calculate_coverage(),
            'avg_similarity': self._calculate_avg_similarity(),
            'model_size': self.recommender.combined_similarity.nnz
        }

        return metrics

    def _calculate_coverage(self):
        """计算覆盖率"""
        n_users = len(self.recommender.user_index)
        n_musics = len(self.recommender.music_index)

        # 可预测的用户-物品对
        predictable_pairs = 0
        total_pairs = n_users * n_musics

        # 简单估算
        for i in range(n_users):
            similarities = self.recommender.combined_similarity[i].toarray().flatten()
            if np.sum(similarities > 0) > 0:  # 有相似用户
                predictable_pairs += n_musics

        coverage = predictable_pairs / total_pairs if total_pairs > 0 else 0
        return coverage

    def _calculate_avg_similarity(self):
        """计算平均相似度"""
        if self.recommender.combined_similarity is None:
            return 0

        similarities = self.recommender.combined_similarity.data
        if len(similarities) > 0:
            return float(np.mean(similarities))
        return 0

    def save(self, model_path='music_cf_model.pkl'):
        """保存整个系统"""
        system_data = {
            'config': self.config,
            'processor': self.processor,
            'recommender': self.recommender,
            'is_trained': self.is_trained
        }

        with open(model_path, 'wb') as f:
            pickle.dump(system_data, f)

        print(f"系统保存到: {model_path}")

    def load(self, model_path='music_cf_model.pkl'):
        """加载系统"""
        with open(model_path, 'rb') as f:
            system_data = pickle.load(f)

        self.config = system_data['config']
        self.processor = system_data['processor']
        self.recommender = system_data['recommender']
        self.is_trained = system_data['is_trained']

        print(f"系统从 {model_path} 加载")