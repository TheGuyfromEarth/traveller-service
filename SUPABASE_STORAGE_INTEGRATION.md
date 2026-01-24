# Supabase Storage Integration Guide

## Overview
The application has been migrated from AWS S3 to Supabase Storage for image management. Supabase Storage is a PostgreSQL-backed, S3-compatible file storage service that integrates seamlessly with your existing Supabase database.

## Changes Made

### 1. New Service Class: `SupabaseStorageService`
**Location:** `src/main/java/com/travolish/traveller/user/service/SupabaseStorageService.java`

This service replaces `S3Service` and provides the following methods:

- **`upload(String path, InputStream inputStream, String contentType): String`**
  - Uploads a file to Supabase Storage
  - Parameters:
    - `path`: File path in storage (e.g., "users/123/profile.jpg")
    - `inputStream`: File content stream
    - `contentType`: MIME type (e.g., "image/jpeg")
  - Returns: Public URL of the uploaded file

- **`getPublicUrl(String path): String`**
  - Returns the public URL for a file
  - Useful for files in public buckets

- **`getPresignedUrl(String path, Duration expiry): URL`**
  - Returns a URL for file access
  - For private/authenticated access, implement Row-Level Security (RLS) in Supabase

- **`delete(String path): void`**
  - Deletes a file from Supabase Storage

### 2. Updated Controller: `ImageController`
**Location:** `src/main/java/com/travolish/traveller/user/controller/ImageController.java`

- Replaced `S3Service` dependency with `SupabaseStorageService`
- Updated upload and download logic to use Supabase API

### 3. Configuration Class: `StorageConfiguration`
**Location:** `src/main/java/com/travolish/traveller/config/StorageConfiguration.java`

- Provides RestTemplate bean for HTTP communication with Supabase
- Configures timeouts (10s connect, 30s read)

### 4. Updated Dependencies
**File:** `build.gradle`

**Removed:**
```gradle
implementation 'software.amazon.awssdk:s3:2.20.120'
```

**Added:**
```gradle
implementation 'org.apache.httpcomponents.client5:httpclient5:5.2.1'
```

### 5. Updated Configuration
**Files:** `src/main/resources/application.yaml` and `bin/main/application.yaml`

**Old S3 config:**
```yaml
spring:
  s3:
    bucket: ${S3_BUCKET:travolish-bucket}
    region: ${AWS_REGION:us-east-1}
```

**New Supabase config:**
```yaml
supabase:
  url: ${SUPABASE_URL:https://your-project.supabase.co}
  api-key: ${SUPABASE_API_KEY:your-anon-key}
  bucket-name: ${SUPABASE_BUCKET_NAME:travolish-bucket}
```

## Setup Instructions

### 1. Create Supabase Project
1. Go to [https://supabase.com](https://supabase.com)
2. Create a new project
3. Note your **Project URL** and **API Key** (anon key)

### 2. Create Storage Bucket
1. In Supabase Dashboard, go to **Storage**
2. Create a new bucket named `travolish-bucket` (or use your preferred name)
3. Configure bucket access policies:
   - For **public** access (anyone can view): Set bucket policies to allow public access
   - For **private** access: Enable Row-Level Security (RLS) and configure policies

### 3. Configure Environment Variables
Set these environment variables in your deployment:

```bash
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_API_KEY=your-anon-key-here
SUPABASE_BUCKET_NAME=travolish-bucket
```

Or update `application.yaml` with your Supabase credentials:

```yaml
supabase:
  url: https://your-project-id.supabase.co
  api-key: your-anon-key
  bucket-name: travolish-bucket
```

### 4. Optional: Enable Row-Level Security (RLS)
For private file access with user authentication:

1. In Supabase Dashboard, go to **Storage** → **Policies**
2. Enable RLS on your bucket
3. Add policy to allow authenticated users to access their own files:

```sql
CREATE POLICY "Users can upload their own images"
ON storage.objects
FOR INSERT
WITH CHECK (auth.uid()::text = (storage.foldername(name))[1]);

CREATE POLICY "Users can view their own images"
ON storage.objects
FOR SELECT
USING (auth.uid()::text = (storage.foldername(name))[1]);
```

## API Endpoints

### Upload Image
```
POST /api/users/{id}/image
Content-Type: multipart/form-data

Body: file (multipart file)
```

**Response:**
- **Status:** 201 Created
- Image stored at path: `users/{id}/profile-{timestamp}.{extension}`

### Get Image
```
GET /api/users/{id}/image
```

**Response:**
- **Status:** 302 Found (redirect to image URL)
- **Location:** Public URL of the image

## Comparison: S3 vs Supabase Storage

| Feature | AWS S3 | Supabase Storage |
|---------|--------|------------------|
| Pricing | Pay-per-use | Integrated with Supabase (cheaper for small projects) |
| Database Integration | Separate service | Integrated with PostgreSQL |
| Setup Complexity | High (IAM roles, policies) | Low (simpler configuration) |
| Authentication | IAM-based | Supabase Auth integration |
| Signed URLs | Built-in support | Via RLS policies |
| SDK | Requires AWS SDK | Simple REST API |

## Migration from S3

If you had existing images in S3, you can migrate them using:

1. **AWS CLI** to download from S3:
```bash
aws s3 sync s3://travolish-bucket . --region us-east-1
```

2. **Supabase CLI** to upload to Supabase:
```bash
supabase storage objects upload travolish-bucket ./users \
  --prefix users/
```

## Troubleshooting

### Error: "Unable to determine Dialect without JDBC metadata"
- Ensure `application.yaml` has the database configuration
- Check `DB_URL`, `DB_USER`, `DB_PASSWORD` environment variables

### Error: "401 Unauthorized" when uploading
- Verify `SUPABASE_API_KEY` is correct
- Check bucket policies allow uploads
- Ensure bucket exists in Supabase

### Error: "File not found" when retrieving
- Verify the file path is correct
- Check if file exists in Supabase bucket
- Ensure bucket is accessible (not private without RLS setup)

### Slow uploads
- Check internet connection
- Verify `RestTemplate` timeout settings in `StorageConfiguration`
- Consider compressing images before upload

## Future Enhancements

1. **Image Optimization:**
   - Compress images before upload
   - Generate thumbnails
   - Support multiple image sizes

2. **Advanced Security:**
   - Implement fine-grained RLS policies
   - Add virus scanning
   - Rate limiting on uploads

3. **CDN Integration:**
   - Use Supabase Storage with CDN for faster delivery
   - Set up image caching headers

4. **Batch Operations:**
   - Support bulk upload/download
   - Implement batch deletion

## Resources

- [Supabase Storage Documentation](https://supabase.com/docs/guides/storage)
- [Supabase REST API Reference](https://supabase.com/docs/reference/javascript/storage-createbucket)
- [Row-Level Security in Supabase](https://supabase.com/docs/guides/auth/row-level-security)
