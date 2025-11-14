import {gql} from '@apollo/client';

export const GET_ALL_COMMENTS = gql`
    query GetAllComments($lang: String!) {
        jcr(workspace: LIVE) {
            nodesByCriteria(criteria: {
                nodeType: "jsblognt:comment",
                language: $lang
            }) {
                nodes {
                    uuid
                    path
                    workspace
                    author: property(name: "author") {
                        value
                    }
                    comment: property(name: "comment") {
                        value
                    }
                    status: property(name: "status") {
                        value
                    }
                    approved: property(name: "approved") {
                        value
                    }
                    created: property(name: "jcr:created") {
                        value
                    }
                }
            }
        }
    }
`;

export const GET_POST_BY_ID = gql`
    query GetPostById($postId: String!, $lang: String!) {
        jcr(workspace: LIVE) {
            nodeById(uuid: $postId) {
                uuid
                workspace
                displayName(language: $lang)
            }
        }
    }
`;

export const GET_POST_BY_PATH = gql`
    query GetPostByPath($postPath: String!, $lang: String!) {
        jcr(workspace: LIVE) {
            nodeByPath(path: $postPath) {
                uuid
                displayName(language: $lang)
            }
        }
    }
`;

export const UPDATE_COMMENT_STATUS = gql`
    mutation UpdateCommentStatus($commentId: String!, $status: String!, $approved: String!) {
        jcr(workspace: LIVE) {
            mutateNode(pathOrId: $commentId) {
                status: mutateProperty(name: "status") {
                    setValue(value: $status)
                }
                approved: mutateProperty(name: "approved") {
                    setValue(value: $approved)
                }
            }
        }
    }
`;

export const DELETE_COMMENT = gql`
    mutation DeleteComment($commentId: String!) {
        jcr(workspace: LIVE) {
            deleteNode(pathOrId: $commentId)
        }
    }
`;
