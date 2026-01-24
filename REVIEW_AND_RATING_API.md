# Review & Rating System - API Documentation

## Overview
The Review & Rating System provides comprehensive functionality for guests to submit reviews, rate hotels and rooms, and includes a moderation workflow for content management.

## Features
- **Guest Reviews**: Submit detailed reviews with ratings (1-5 stars) for hotels and rooms
- **Rating Aggregation**: Automatic calculation of average ratings and distribution
- **Duplicate Prevention**: Users cannot submit multiple reviews for the same hotel/room
- **Review Moderation**: 3-stage workflow (PENDING → APPROVED/REJECTED → FLAGGED for escalation)
- **Helpful Voting**: Users can mark reviews as helpful or unhelpful
- **Review Statistics**: Comprehensive rating analytics and breakdowns

---

## Core Entities

### Review Entity
```
Review {
  id: Long,
  userId: Long,
  hotelId: Long,
  roomId: Long (optional),
  title: String,
  content: String,
  rating: Integer (1-5),
  status: ReviewStatus (PENDING, APPROVED, REJECTED, FLAGGED),
  reviewType: ReviewType (HOTEL, ROOM),
  moderatorNotes: String,
  moderatorId: Long,
  reviewedAt: OffsetDateTime,
  helpfulCount: Integer,
  unhelpfulCount: Integer,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime
}
```

### ReviewStatus Workflow
- **PENDING**: Initial status - awaiting moderator review
- **APPROVED**: Visible to public and counted in ratings
- **REJECTED**: Not visible; reason provided in moderatorNotes
- **FLAGGED**: Escalated for further investigation

### RatingStatsDTO
```
RatingStatsDTO {
  entityId: Long,
  entityType: String (HOTEL or ROOM),
  averageRating: Double,
  totalReviews: Long,
  oneStar: Long,
  twoStars: Long,
  threeStars: Long,
  fourStars: Long,
  fiveStars: Long,
  percentageRating: Double (% of 5-star reviews)
}
```

---

## API Endpoints

### 1. Submit Reviews

#### Submit Hotel Review
```
POST /api/reviews/hotels/{hotelId}
Headers: X-User-Id: {userId}
Body:
{
  "title": "Great experience!",
  "content": "The hotel was clean and staff was very helpful...",
  "rating": 5
}

Response: 201 Created
{
  "id": 1,
  "userId": 5,
  "hotelId": 10,
  "title": "Great experience!",
  "content": "The hotel was clean and staff was very helpful...",
  "rating": 5,
  "status": "PENDING",
  "reviewType": "HOTEL",
  "createdAt": "2025-11-30T10:30:00Z",
  "updatedAt": "2025-11-30T10:30:00Z"
}
```

#### Submit Room Review
```
POST /api/reviews/hotels/{hotelId}/rooms/{roomId}
Headers: X-User-Id: {userId}
Body:
{
  "title": "Comfortable and spacious",
  "content": "Good bed quality and modern amenities...",
  "rating": 4
}

Response: 201 Created
```

---

### 2. Retrieve Reviews

#### Get Specific Review
```
GET /api/reviews/{reviewId}

Response: 200 OK
{
  "id": 1,
  "userId": 5,
  "hotelId": 10,
  "title": "Great experience!",
  "content": "...",
  "rating": 5,
  "status": "APPROVED",
  "reviewType": "HOTEL",
  "helpfulCount": 12,
  "unhelpfulCount": 2,
  "createdAt": "2025-11-30T10:30:00Z"
}
```

#### Get Hotel Reviews (Paginated)
```
GET /api/reviews/hotels/{hotelId}?page=0&size=10&sort=createdAt,desc

Response: 200 OK
{
  "content": [
    { review1 },
    { review2 }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 45,
  "totalPages": 5
}
```

#### Get Room Reviews (Paginated)
```
GET /api/reviews/rooms/{roomId}?page=0&size=10

Response: 200 OK (same structure as hotel reviews)
```

#### Get User's Reviews
```
GET /api/reviews/user
Headers: X-User-Id: {userId}

Response: 200 OK (paginated list of user's reviews)
```

---

### 3. Rating Statistics & Analytics

#### Get Hotel Rating Statistics
```
GET /api/reviews/hotels/{hotelId}/stats

Response: 200 OK
{
  "entityId": 10,
  "entityType": "HOTEL",
  "averageRating": 4.35,
  "totalReviews": 45,
  "oneStar": 2,
  "twoStars": 3,
  "threeStars": 8,
  "fourStars": 18,
  "fiveStars": 14,
  "percentageRating": 31.11
}
```

#### Get Room Rating Statistics
```
GET /api/reviews/rooms/{roomId}/stats

Response: 200 OK (same structure)
```

---

### 4. Review Management

#### Update Review
```
PUT /api/reviews/{reviewId}
Body:
{
  "title": "Updated title",
  "content": "Updated content",
  "rating": 4
}

Response: 200 OK
Note: Only PENDING/REJECTED reviews can be updated
```

#### Delete Review
```
DELETE /api/reviews/{reviewId}

Response: 204 No Content
```

---

### 5. Review Moderation (Admin/Moderator Only)

#### Approve Review
```
POST /api/reviews/{reviewId}/approve
Headers: X-Moderator-Id: {moderatorId}

Response: 200 OK
{
  "id": 1,
  "status": "APPROVED",
  "moderatorId": 2,
  "reviewedAt": "2025-11-30T11:00:00Z"
}
```

#### Reject Review
```
POST /api/reviews/{reviewId}/reject?reason=Inappropriate+language
Headers: X-Moderator-Id: {moderatorId}

Response: 200 OK
{
  "id": 1,
  "status": "REJECTED",
  "moderatorNotes": "Inappropriate language",
  "moderatorId": 2,
  "reviewedAt": "2025-11-30T11:00:00Z"
}
```

#### Flag Review
```
POST /api/reviews/{reviewId}/flag

Response: 200 OK
{
  "id": 1,
  "status": "FLAGGED"
}
```

#### Get Pending Reviews
```
GET /api/reviews/moderation/pending?page=0&size=20

Response: 200 OK
{
  "content": [
    { pending_review_1 },
    { pending_review_2 }
  ],
  "totalElements": 15
}
```

#### Get Flagged Reviews
```
GET /api/reviews/moderation/flagged?page=0&size=20

Response: 200 OK (paginated list of flagged reviews)
```

---

### 6. Helpful/Unhelpful Voting

#### Mark Review as Helpful
```
POST /api/reviews/{reviewId}/helpful

Response: 200 OK
{
  "id": 1,
  "helpfulCount": 13,
  "unhelpfulCount": 2
}
```

#### Mark Review as Unhelpful
```
POST /api/reviews/{reviewId}/unhelpful

Response: 200 OK
{
  "id": 1,
  "helpfulCount": 12,
  "unhelpfulCount": 3
}
```

---

### 7. Review Eligibility

#### Check if User Can Review Hotel
```
GET /api/reviews/hotels/{hotelId}/can-review
Headers: X-User-Id: {userId}

Response: 200 OK
true/false
```

#### Check if User Can Review Room
```
GET /api/reviews/rooms/{roomId}/can-review
Headers: X-User-Id: {userId}

Response: 200 OK
true/false
```

---

## Validation Rules

### Review Submission
- **Rating**: Must be between 1 and 5 (inclusive)
- **Title**: Required, max 200 characters, cannot be blank
- **Content**: Required, max 5000 characters, cannot be blank
- **Duplicate Check**: User cannot submit multiple reviews for same hotel/room
- **Review Type**: Automatically set based on endpoint (HOTEL or ROOM)

### Status Transitions
- PENDING → APPROVED (via approval)
- PENDING → REJECTED (via rejection with reason)
- APPROVED → FLAGGED (escalation)
- FLAGGED → APPROVED or REJECTED (after review)

### Update Restrictions
- Only PENDING or REJECTED reviews can be updated
- APPROVED reviews cannot be modified (must delete and create new)
- Reviewer is the only one who can update their own review

---

## Error Handling

### Common Error Responses

#### 400 Bad Request
```json
{
  "status": 400,
  "message": "Rating must be between 1 and 5",
  "timestamp": "2025-11-30T10:30:00Z"
}
```

#### 409 Conflict
```json
{
  "status": 409,
  "message": "User has already reviewed this hotel. Please update your existing review."
}
```

#### 404 Not Found
```json
{
  "status": 404,
  "message": "Review not found with id: 999"
}
```

---

## Business Logic

### Rating Aggregation
- Reviews are counted only when status = APPROVED
- Average rating = sum of all ratings / total approved reviews
- Percentage rating = (5-star reviews / total reviews) × 100

### Review Moderation Workflow
1. **Initial**: Review is submitted with status = PENDING
2. **Moderation Queue**: Review appears in `/moderation/pending`
3. **Decision**: Moderator approves or rejects
4. **Escalation**: If issues arise, can be flagged for re-review
5. **Final State**: APPROVED (visible) or REJECTED (hidden)

### Duplicate Prevention
- Each user can only submit ONE review per hotel
- Each user can only submit ONE review per room
- Attempting to submit duplicate review throws InvalidReviewException
- User must update existing review or delete it first

### Helpful Voting
- Each review tracks separate counts for helpful/unhelpful
- Votes are incremental (no duplicate prevention)
- Used to rank reviews by usefulness

---

## Integration Examples

### Frontend: Display Hotel Reviews
```javascript
// 1. Get rating statistics
const stats = await fetch('/api/reviews/hotels/10/stats').then(r => r.json());
console.log(`Average Rating: ${stats.averageRating}/5 (${stats.totalReviews} reviews)`);

// 2. Get paginated reviews
const reviews = await fetch('/api/reviews/hotels/10?page=0&size=5').then(r => r.json());
reviews.content.forEach(review => {
  console.log(`${review.title}: ${review.rating}⭐`);
});
```

### Moderator: Review Pending Reviews
```javascript
// Get pending reviews
const pending = await fetch('/api/reviews/moderation/pending?page=0&size=20').then(r => r.json());

// Approve a review
await fetch('/api/reviews/1/approve', {
  method: 'POST',
  headers: { 'X-Moderator-Id': '2' }
});

// Reject a review with reason
await fetch('/api/reviews/2/reject?reason=Spam', {
  method: 'POST',
  headers: { 'X-Moderator-Id': '2' }
});
```

---

## Performance Considerations

### Database Indexes
- `reviews(hotelId, status)` - Common filter for hotel statistics
- `reviews(roomId, status)` - Common filter for room statistics
- `reviews(userId)` - User's review history
- `reviews(status)` - Moderation queue queries

### Caching Recommendations
- Cache RatingStatsDTO with 1-hour TTL
- Invalidate cache on review approval/rejection
- Cache approved review pages with 30-minute TTL

---

## Future Enhancements
- Photo uploads with reviews
- Review filtering by date range
- Review search and full-text search
- Reviewer verification badges
- Review response by hotel management
- Review analytics and trends
- Review highlighting for specific aspects (cleanliness, service, etc.)

