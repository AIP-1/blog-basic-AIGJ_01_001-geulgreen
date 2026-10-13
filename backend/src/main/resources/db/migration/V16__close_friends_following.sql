-- 069 후속(강성찬 개인 확장): 친구는 내가 팔로우하는 사람만. V15에서 옮긴 맞친구 중 내가 팔로우하지 않는 사람은 친구에서, 그리고 내 그룹에서 뺀다.
DELETE FROM close_friend cf
WHERE NOT EXISTS (SELECT 1 FROM follow f WHERE f.follower_id = cf.owner_id AND f.followee_id = cf.friend_id);

DELETE FROM group_member gm USING friend_group g
WHERE gm.group_id = g.id
  AND NOT EXISTS (SELECT 1 FROM close_friend cf WHERE cf.owner_id = g.owner_id AND cf.friend_id = gm.member_id);
