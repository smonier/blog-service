# Blog Service Module

A GraphQL extension module for Jahia that provides UGC (User Generated Content) services for blog comments and likes. This module extends the blog-factory module by handling user interactions through GraphQL mutations.

## Overview

This module is designed similar to the vote-service pattern, providing:

- **GraphQL Mutations**: Comment and like operations for blog posts
- **UGC Storage**: Stores comments and likes under `/sites/*/contents/ugc/blogs/` in the LIVE workspace
- **Duplicate Prevention**: Client and IP-based hash validation to prevent spam
- **CSRF Protection**: Validates X-CSRF-Token header on all mutations

## Architecture

### OSGi Configuration

#### BlogConfigurationService
Manages runtime configuration with the following settings:

- **serverSecret**: Secret used to hash client identifiers and IPs (must be changed in production)
- **clientIdCookieName**: Name of the cookie containing client identifier (default: `jahia-client-id`)
- **enableIpHash**: Whether to hash and store IP addresses (default: `true`)
- **requireModeration**: Whether comments need approval before being visible (default: `true`)

Configuration file: `META-INF/configurations/org.jahia.se.modules.blogservice.cfg`

**Important**: Change the `serverSecret` value in production and rotate it periodically for security.

### GraphQL Extensions

#### BlogMutations
Exposes three main mutations:
- `blog.addComment`: Add a comment to a blog post
- `blog.addLike`: Add a like to a blog post
- `blog.ratePost`: Rate a blog post (1-5 stars)

All mutations:
- Validate CSRF tokens
- Generate client and IP hashes for duplicate detection
- Store UGC data in the LIVE workspace
- Return success/failure payloads with status codes

### UGC Services

#### BlogCommentService
Handles comment persistence:
- Creates comment nodes under `/sites/{site}/contents/ugc/blogs/{postId}/comments/`
- Validates against duplicate comments (same content + hash within 1 minute)
- Sets comments to require approval by default
- Returns comment ID for moderation workflows

#### BlogLikeService
Handles like persistence:
- Creates like nodes under `/sites/{site}/contents/ugc/blogs/{postId}/likes/`
- Prevents duplicate likes from same client/IP
- Tracks timestamps and user agents

#### BlogRatingService
Handles rating persistence and calculation:
- Creates/updates rating nodes under `/sites/{site}/contents/ugc/blogs/{postId}/ratings/`
- Users can update their existing rating (not duplicate)
- Calculates average rating and total count in real-time
- Ratings are integers from 1 to 5

### JCR Node Types

Defined in `definitions.cnd`:

- **jsblognt:comment**: Comment node with content, author info, hashes, and approval status
- **jsblognt:commentsFolder**: Container for comments
- **jsblognt:like**: Like node with hashes and timestamps
- **jsblognt:likesFolder**: Container for likes
- **jsblognt:rating**: Rating node with rating value (1-5), hashes, and timestamps
- **jsblognt:ratingsFolder**: Container for ratings

## GraphQL API

### Queries

#### Get Comments

Retrieve all comments for a blog post:

```graphql
query GetComments($postId: String!) {
  blog {
    getComments(postId: $postId) {
      postId
      comments {
        uuid
        authorName
        body
        created
        status
      }
      total
    }
  }
}
```

**Variables:**
```json
{
  "postId": "040f0e5f-7a5b-43e7-a415-fe0d7429e07b"
}
```

**Response:**
```json
{
  "data": {
    "blog": {
      "getComments": {
        "postId": "040f0e5f-7a5b-43e7-a415-fe0d7429e07b",
        "comments": [
          {
            "uuid": "comment-uuid",
            "authorName": "John Doe",
            "body": "Great article!",
            "created": "2025-11-13T17:30:00Z",
            "status": "APPROVED"
          }
        ],
        "total": 1
      }
    }
  }
}
```

**Comment Status Values:**
- `APPROVED`: Comment is visible to all users
- `AWAITING_MODERATION`: Comment needs approval

### Mutations

### Add Comment

```graphql
mutation {
  blog {
    addComment(
      blogPostId: "uuid-of-blog-post"
      comment: "Great article!"
      author: "John Doe"
      authorEmail: "john@example.com"
      clientHash: "optional-client-identifier"
    ) {
      success
      code
      commentId
    }
  }
}
```

**Response Codes:**
- `OK`: Comment added successfully
- `DUPLICATE_COMMENT`: Same comment already submitted
- `AWAITING_MODERATION`: Comment requires approval

### Add Like

```graphql
mutation {
  blog {
    addLike(
      blogPostId: "uuid-of-blog-post"
      clientHash: "optional-client-identifier"
    ) {
      success
      code
    }
  }
}
```

**Response Codes:**
- `OK`: Like added successfully
- `ALREADY_LIKED`: User already liked this post

### Rate Post

```graphql
mutation RatePost($postId: String!, $rating: Int!) {
  blog {
    ratePost(postId: $postId, rating: $rating) {
      postId
      averageRating
      ratingCount
    }
  }
}
```

**Variables:**
```json
{
  "postId": "040f0e5f-7a5b-43e7-a415-fe0d7429e07b",
  "rating": 5
}
```

**Headers:**
```json
{
  "X-CSRF-Token": "your-csrf-token"
}
```

**Response:**
```json
{
  "data": {
    "blog": {
      "ratePost": {
        "postId": "040f0e5f-7a5b-43e7-a415-fe0d7429e07b",
        "averageRating": 4.5,
        "ratingCount": 10
      }
    }
  }
}
```

**Notes:**
- Rating must be an integer between 1 and 5
- Users can update their existing rating (not create duplicate ratings)
- Returns the new average rating and total count after submission

## Security Features

### CSRF Protection
All mutations require the `X-CSRF-Token` header to be present.

### Server Secret
The configuration service provides a server secret that is used in hash generation:
- `hash = SHA256(blogPostId + ":" + clientId + ":" + serverSecret)`
- Default secret MUST be changed in production
- Secret should be rotated periodically

### Duplicate Prevention
Uses SHA-256 hashes of:
1. **Client Identifier**: From cookie (configurable name) or provided hash, combined with server secret
2. **IP Address**: Truncated IP (first 3 octets for IPv4, first 4 segments for IPv6), combined with server secret

### Comment Moderation
- Configurable via `requireModeration` setting
- When enabled (default), comments are created with `approved=false`
- When disabled, comments are auto-approved

## Configuration

### OSGi Configuration File

Edit `karaf/etc/org.jahia.se.modules.blogservice.cfg`:

```properties
# IMPORTANT: Change this secret in production!
serverSecret=your-strong-secret-here-rotate-regularly

# Cookie name for client identifier
clientIdCookieName=jahia-client-id

# Enable IP-based duplicate detection
enableIpHash=true

# Require manual approval for comments
requireModeration=true
```

### Runtime Configuration
Configuration can also be managed through Jahia's OSGi Configuration Manager in the Tools area.

## UGC Storage Structure

```
/sites/{siteKey}/contents/ugc/blogs/
├── {blogPostId}/
│   ├── comments/
│   │   ├── c-{uuid} (jsblognt:comment)
│   │   └── c-{uuid} (jsblognt:comment)
│   └── likes/
│       ├── l-{uuid} (jsblognt:like)
│       └── l-{uuid} (jsblognt:like)
```

All UGC is stored in the **LIVE workspace** with system session privileges.

## Dependencies

- `graphql-dxm-provider` 3.4.0 - Jahia GraphQL provider
- `graphql-java-annotations` - GraphQL annotations support
- `commons-lang3` 3.12.0 - String utilities
- `javax.inject` - Dependency injection
- `javax.servlet-api` - HTTP request handling

## Building the Module

```bash
mvn clean install
```

This will:
1. Compile all Java sources
2. Process CND definitions
3. Package as OSGi bundle with DS annotations
4. Generate module JAR

## Deployment

1. Deploy the JAR to Jahia's `modules` directory
2. The module will auto-install and register GraphQL extensions
3. Mutations are immediately available at `/modules/graphql` endpoint

## Integration with blog-factory

The blog-factory module should:

1. **Call GraphQL mutations** from frontend to add comments/likes
2. **Query UGC data** to display comments and like counts
3. **Define blog post content types** (blog-service only handles UGC)

Example frontend integration:

```javascript
// Retrieve comments
const { data } = await jahiaGQL.query({
  query: gql`
    query GetComments($postId: String!) {
      blog {
        getComments(postId: $postId) {
          postId
          comments {
            uuid
            authorName
            body
            created
            status
          }
          total
        }
      }
    }
  `,
  variables: { postId }
});

// Add comment
const result = await jahiaGQL.mutate({
  mutation: gql`
    mutation addComment($postId: String!, $comment: String!, $author: String!) {
      blog {
        addComment(blogPostId: $postId, comment: $comment, author: $author) {
          success
          code
          commentId
        }
      }
    }
  `,
  variables: { postId, comment, author },
  context: {
    headers: {
      'X-CSRF-Token': csrfToken
    }
  }
});

// Rate post
const ratingResult = await jahiaGQL.mutate({
  mutation: gql`
    mutation RatePost($postId: String!, $rating: Int!) {
      blog {
        ratePost(postId: $postId, rating: $rating) {
          postId
          averageRating
          ratingCount
        }
      }
    }
  `,
  variables: { postId, rating: 5 },
  context: {
    headers: {
      'X-CSRF-Token': csrfToken
    }
  }
});
```

## Package Structure

```
org.jahia.se.modules.blogservice
├── graphql/
│   ├── BlogGraphQLExtensionsProvider.java  - GraphQL extension registration
│   ├── BlogQueriesExtension.java            - Query extension point
│   ├── BlogQueries.java                     - Query implementations
│   ├── BlogMutationsExtension.java          - Mutation extension point
│   ├── BlogMutations.java                   - Mutation implementations
│   ├── Comment.java                         - Comment DTO for queries
│   ├── CommentsPayload.java                 - Comments list response
│   ├── CommentPayload.java                  - Comment mutation response
│   ├── LikePayload.java                     - Like response
│   └── RatingPayload.java                   - Rating response
├── services/
│   ├── BlogCommentService.java              - Comment UGC service
│   ├── BlogLikeService.java                 - Like UGC service
│   ├── BlogRatingService.java               - Rating UGC service
│   ├── BlogConfigurationService.java        - OSGi configuration service
│   ├── BlogServiceException.java            - Service exception
│   ├── CommentRequest.java                  - Comment request builder
│   ├── CommentResult.java                   - Comment result
│   ├── LikeRequest.java                     - Like request builder
│   ├── LikeResult.java                      - Like result
│   ├── RatingRequest.java                   - Rating request builder
│   └── RatingResult.java                    - Rating result
└── util/
    ├── HashUtils.java                       - SHA-256 hashing
    ├── IpUtils.java                         - IP extraction and truncation
    └── RequestUtil.java                     - HTTP request extraction
```

## Utility Functions

### HashUtils
- `sha256(String)`: Generate SHA-256 hash for duplicate detection

### IpUtils
- `extractClientIp(HttpServletRequest)`: Extract real client IP from headers/proxy
- `truncateForHash(String)`: Truncate IP for privacy (keeps first 3-4 segments)

### RequestUtil
- `extractHttpServletRequest(DataFetchingEnvironment)`: Extract servlet request from GraphQL context

## Error Handling

- **Repository Exceptions**: Logged and wrapped in BlogServiceException
- **Missing CSRF**: Returns DataFetchingException
- **Duplicate Detection**: Returns failure payload with appropriate code
- **System Session**: All operations use system session for UGC persistence

## Logging

Uses SLF4J for logging:
- **INFO**: Comment/like submissions, folder creation
- **DEBUG**: Path resolution, node type information
- **ERROR**: Repository exceptions, persistence failures

## Notes

Compile errors in the IDE are expected during development and will resolve once:
1. The module is built with Maven (dependencies resolved)
2. Deployed to Jahia runtime (Jahia APIs available)
3. OSGi container activates the bundle

This module follows the same pattern as vote-service and is specifically designed to handle UGC for blog posts defined in the blog-factory module.

