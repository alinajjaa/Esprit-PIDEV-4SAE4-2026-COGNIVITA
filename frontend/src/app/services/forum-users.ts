export const FORUM_TEST_USERS = ['alice', 'bob', 'charlie', 'diana', 'moderator'] as const;

export type ForumTestUser = (typeof FORUM_TEST_USERS)[number];

