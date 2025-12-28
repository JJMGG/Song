# # 数据库配置
# from MusicHybridRecommender import MusicHybridRecommender
#
# db_config = {
#     'host': 'localhost',
#     'port': 3306,
#     'user': 'root',
#     'password': '123456',
#     'database': 'mysong',
#     'charset': 'utf8mb4'
# }
#
# # 创建推荐系统
# recommender = MusicHybridRecommender(db_config, {
#     'k_neighbors': 2,  # 由于数据少，邻居数设小
#     'similarity_threshold': 0.01,  # 降低阈值
#     'min_common_items': 1,
#     'min_interactions': 1,
#     'music_recommend_ratio': 0.6,
#     'playlist_recommend_ratio': 0.4
# })
#
# # 1. 加载数据
# if recommender.load_data():
#     print("\n✓ 数据加载成功")
#
#     # 2. 预处理数据
#     if recommender.preprocess_data():
#         print("\n✓ 数据预处理成功")
#
#         # 3. 模型评估
#         recommender.evaluate()
#
#         # 4. 为用户2生成推荐
#         test_user_id = 2
#
#         # 获取用户信息
#         print("\n" + "=" * 50)
#         print(f"用户 {test_user_id} 信息:")
#         print("=" * 50)
#
#         user_info = recommender.get_user_info(test_user_id)
#         print(f"是否在索引中: {'是' if user_info['is_in_index'] else '否'}")
#         print(f"音乐交互次数: {user_info['music_interactions']}")
#         print(f"听过的音乐: {user_info['listened_music']}")
#         print(f"歌单交互次数: {user_info['playlist_interactions']}")
#         print(f"交互的歌单: {user_info['interacted_playlists']}")
#
#         # 4.1 协同过滤推荐
#         print("\n" + "=" * 50)
#         print("1. 协同过滤推荐:")
#         print("=" * 50)
#
#         cf_music_recs = recommender.recommend_music_cf(test_user_id, 5)
#         cf_playlist_recs = recommender.recommend_playlists_cf(test_user_id, 3)
#
#         print("\n协同过滤歌曲推荐:")
#         if cf_music_recs:
#             for i, rec in enumerate(cf_music_recs, 1):
#                 print(f"  {i}. 歌曲{rec['id']} (得分: {rec['score']:.2f}, 理由: {rec['reason']})")
#         else:
#             print("  无歌曲推荐")
#
#         print("\n协同过滤歌单推荐:")
#         if cf_playlist_recs:
#             for i, rec in enumerate(cf_playlist_recs, 1):
#                 print(f"  {i}. 歌单{rec['id']}: {rec['name']} (得分: {rec['score']:.2f}, 理由: {rec['reason']})")
#         else:
#             print("  无歌单推荐")
#
#         # 4.2 增强推荐
#         print("\n" + "=" * 50)
#         print("2. 增强推荐:")
#         print("=" * 50)
#
#         enhanced_music_recs = recommender.recommend_music_enhanced(test_user_id, 5)
#
#         print("\n增强歌曲推荐:")
#         if enhanced_music_recs:
#             for i, rec in enumerate(enhanced_music_recs, 1):
#                 print(f"  {i}. 歌曲{rec['id']} (得分: {rec['score']:.2f}, 算法: {rec.get('algorithm', 'unknown')})")
#         else:
#             print("  无增强推荐")
#
#         # 4.3 混合推荐
#         print("\n" + "=" * 50)
#         print("3. 混合推荐:")
#         print("=" * 50)
#
#         hybrid_recs = recommender.hybrid_recommend(test_user_id, 20)
#
#         print("\n最终混合推荐结果:")
#         for i, rec in enumerate(hybrid_recs, 1):
#             if rec['type'] == 'music':
#                 print(f"  {i}. [歌曲] 歌曲{rec['id']} (得分: {rec['score']:.2f})")
#                 print(f"      理由: {rec['reason']}")
#             else:
#                 print(f"  {i}. [歌单] 歌单{rec['id']}: {rec['name']} (得分: {rec['score']:.2f})")
#                 print(f"      包含{rec['music_count']}首歌曲, 理由: {rec['reason']}")
#
#         # 5. 批量推荐示例
#         print("\n" + "=" * 50)
#         print("批量推荐示例:")
#         print("=" * 50)
#
#         all_users = list(recommender.user_index.keys())
#         print(f"所有用户: {all_users}")
#
#         for user_id in all_users[:3]:  # 为前3个用户生成推荐
#             print(f"\n用户 {user_id}:")
#
#             # 简单推荐
#             simple_music_recs = recommender.recommend_music_cf(user_id, 2)
#             simple_playlist_recs = recommender.recommend_playlists_cf(user_id, 2)
#
#             if simple_music_recs:
#                 print(f"  歌曲推荐: {[rec['id'] for rec in simple_music_recs]}")
#             if simple_playlist_recs:
#                 print(f"  歌单推荐: {[rec['id'] for rec in simple_playlist_recs]}")
#     else:
#         print("\n✗ 数据预处理失败")
# else:
#     print("\n✗ 数据加载失败")


from flask import Flask, request, jsonify
import traceback

from MusicHybridRecommender import MusicHybridRecommender

app = Flask(__name__)
recommender = None


def initialize_recommender():
    """初始化推荐器"""
    global recommender
    db_config = {
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': '123456',
        'database': 'mysong',
        'charset': 'utf8mb4'
    }

    config = {
        'k_neighbors': 2,
        'similarity_threshold': 0.01,
        'min_common_items': 1,
        'min_interactions': 5,
        'music_recommend_ratio': 0.6,
        'playlist_recommend_ratio': 0.4
    }

    recommender = MusicHybridRecommender(db_config, config)

    # 加载数据
    if recommender.load_data():
        if recommender.preprocess_data():
            print("推荐系统初始化成功")
            return True
        else:
            print("推荐系统预处理失败")
            return False
    else:
        print("推荐系统数据加载失败")
        return False


@app.route('/recommend/music', methods=['GET'])
def recommend_cf_music():
    """协同过滤歌曲推荐"""
    try:
        user_id = request.args.get('user_id', type=int)
        n = request.args.get('n', 10, type=int)

        if not user_id:
            return jsonify({'error': '缺少user_id参数'}), 400

        recommendations = recommender.recommend_music_cf(user_id, n)

        return jsonify({
            'user_id': user_id,
            'type': 'music',
            'algorithm': 'collaborative_filtering',
            'recommendations': recommendations,
            'count': len(recommendations)
        })

    except Exception as e:
        return jsonify({'error': str(e), 'traceback': traceback.format_exc()}), 500


@app.route('/recommend/playlist', methods=['GET'])
def recommend_cf_playlist():
    """协同过滤歌单推荐"""
    try:
        user_id = request.args.get('user_id', type=int)
        n = request.args.get('n', 8, type=int)

        if not user_id:
            return jsonify({'error': '缺少user_id参数'}), 400

        recommendations = recommender.recommend_playlists_cf(user_id, n)

        return jsonify({
            'user_id': user_id,
            'type': 'playlist',
            'algorithm': 'collaborative_filtering',
            'recommendations': recommendations,
            'count': len(recommendations)
        })

    except Exception as e:
        return jsonify({'error': str(e), 'traceback': traceback.format_exc()}), 500


@app.route('/recommend/music', methods=['GET'])
def recommend_enhanced_music():
    """增强歌曲推荐"""
    try:
        user_id = request.args.get('user_id', type=int)
        n = request.args.get('n', 10, type=int)

        if not user_id:
            return jsonify({'error': '缺少user_id参数'}), 400

        recommendations = recommender.recommend_music_enhanced(user_id, n)

        return jsonify({
            'user_id': user_id,
            'type': 'music',
            'algorithm': 'enhanced',
            'recommendations': recommendations,
            'count': len(recommendations)
        })

    except Exception as e:
        return jsonify({'error': str(e), 'traceback': traceback.format_exc()}), 500


@app.route('/recommend/hybrid', methods=['GET'])
def recommend_hybrid():
    """混合推荐"""
    try:
        user_id = request.args.get('user_id', type=int)
        n = request.args.get('n', 10, type=int)

        if not user_id:
            return jsonify({'error': '缺少user_id参数'}), 400

        recommendations = recommender.hybrid_recommend(user_id, n)

        return jsonify({
            'user_id': user_id,
            'type': 'hybrid',
            'recommendations': recommendations,
            'count': len(recommendations)
        })

    except Exception as e:
        return jsonify({'error': str(e), 'traceback': traceback.format_exc()}), 500


@app.route('/user/info', methods=['GET'])
def user_info():
    """用户信息"""
    try:
        user_id = request.args.get('user_id', type=int)

        if not user_id:
            return jsonify({'error': '缺少user_id参数'}), 400

        info = recommender.get_user_info(user_id)

        return jsonify({
            'user_id': user_id,
            'info': info
        })

    except Exception as e:
        return jsonify({'error': str(e), 'traceback': traceback.format_exc()}), 500


@app.route('/system/evaluate', methods=['GET'])
def system_evaluate():
    """系统评估"""
    try:
        metrics = recommender.evaluate()

        return jsonify({
            'status': 'success',
            'metrics': metrics
        })

    except Exception as e:
        return jsonify({'error': str(e), 'traceback': traceback.format_exc()}), 500


@app.route('/system/status', methods=['GET'])
def system_status():
    """系统状态"""
    try:
        status = {
            'initialized': recommender is not None,
            'is_loaded': recommender.is_loaded if recommender else False,
            'user_count': len(recommender.user_index) if recommender else 0,
            'music_count': len(recommender.music_index) if recommender else 0,
            'playlist_count': len(recommender.playlist_index) if recommender else 0
        }

        return jsonify(status)

    except Exception as e:
        return jsonify({'error': str(e), 'traceback': traceback.format_exc()}), 500


if __name__ == '__main__':
    # 初始化推荐系统
    if initialize_recommender():
        app.run(host='127.0.0.1', port=5000, debug=True)
    else:
        print("推荐系统初始化失败，服务无法启动")

