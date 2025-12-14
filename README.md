# Delivery Management System - Backend API

A comprehensive, enterprise-grade RESTful API for managing delivery operations, built with Spring Boot. This backend service provides secure authentication, real-time order tracking, payment processing, and administrative capabilities for a complete delivery management platform.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Security Features](#security-features)
- [Database Schema](#database-schema)
- [Testing](#testing)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

This backend API serves as the core engine for a modern delivery management system, supporting multiple user roles (Customers, Drivers, Administrators) with comprehensive features including order management, real-time tracking, payment processing, and analytics.

The system is designed with security, scalability, and maintainability as core principles, implementing industry best practices for authentication, authorization, and data protection.

## ✨ Features

### Core Functionality
- **User Management**: Registration, authentication, email verification, password reset
- **Order Management**: Create, track, update, and manage delivery orders
- **Real-time Tracking**: WebSocket-based live location tracking for drivers
- **Payment Integration**: Secure payment processing via Chapa payment gateway
- **Driver Management**: Driver registration, assignment, and performance tracking
- **Administrative Dashboard**: Comprehensive admin panel for system management
- **Analytics & Reporting**: Business intelligence and performance metrics

### Advanced Features
- **Multi-role Authorization**: Role-based access control (RBAC) for Customers, Drivers, and Admins
- **Real-time Notifications**: WebSocket-based push notifications
- **In-app Messaging**: Customer-driver communication system
- **Delivery Proof**: Image upload and verification for completed deliveries
- **Dynamic Pricing**: Configurable pricing models based on distance, weight, and service type
- **Address Management**: Saved and recent address tracking
- **FAQ Management**: Dynamic FAQ system for customer support

## 🛠 Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Build Tool**: Maven
- **Database**: H2 (Development), PostgreSQL (Production)
- **Caching**: Redis
- **Security**: Spring Security with JWT
- **WebSocket**: Spring WebSocket (STOMP)
- **Payment Gateway**: Chapa API
- **Email Service**: SMTP (Gmail)
- **File Storage**: Cloudinary / Local Storage
- **API Documentation**: SpringDoc OpenAPI (Swagger)

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK)**: Version 21 or higher
- **Maven**: Version 3.6+ 
- **Redis**: Version 6.0+ (for caching and session management)
- **PostgreSQL**: Version 12+ (for production)
- **Git**: For version control

### Optional Tools
- **Postman** or **Insomnia**: For API testing
- **IntelliJ IDEA** or **Eclipse**: IDE for development

## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd delivery
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Create environment file**
   ```bash
   cp .env.example .env
   ```

4. **Configure environment variables** (see [Configuration](#configuration) section)

5. **Start Redis server**
   ```bash
   redis-server
   ```

6. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

The API will be available at `http://localhost:8080`

## ⚙️ Configuration

### Environment Variables

Create a `.env` file in the project root with the following variables:

```env
# JWT Configuration
JWT_SECRET=your-secret-key-minimum-32-characters-long
JWT_EXPIRATION=1800000
JWT_REFRESH_EXPIRATION=604800000

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/delivery_db
SPRING_DATASOURCE_USERNAME=your_db_username
SPRING_DATASOURCE_PASSWORD=your_db_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL_ENABLED=false

# Email Configuration (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=465
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@yourdomain.com
MAIL_ADMIN=admin@yourdomain.com

# Payment Gateway (Chapa)
CHAPA_SECRET=your-chapa-secret-key
CHAPA_PUBLIC=your-chapa-public-key
CHAPA_MODE=SANDBOX
CHAPA_BASE_URL=https://api.chapa.co/v1
PAYMENT_RETURN_URL=http://localhost:3000/payments/result
PAYMENT_NOTIFY_URL=http://localhost:8080/api/payments/webhook

# Cloudinary (Optional - for image storage)
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Proxy Configuration (for production)
TRUST_PROXY=false
```

### Application Properties

Key configuration files:
- `src/main/resources/application.properties`: Main application configuration
- `.env`: Environment-specific variables (not committed to version control)

## 🏃 Running the Application

### Development Mode

```bash
mvn spring-boot:run
```

### Production Mode

```bash
mvn clean package
java -jar target/delivery-0.0.1-SNAPSHOT.jar
```

### Using Docker (Optional)

```bash
docker-compose up -d
```

## 📚 API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

**Note**: API documentation requires ADMIN role authentication in production.

### Main API Endpoints

#### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - User logout
- `POST /api/auth/verify-email` - Email verification
- `POST /api/auth/forgot-password` - Password reset request
- `POST /api/auth/reset-password` - Password reset

#### Orders
- `POST /api/orders` - Create new order
- `GET /api/orders/my` - Get user's orders
- `GET /api/orders/{orderNumber}` - Get order details
- `PATCH /api/orders/{orderNumber}/cancel` - Cancel order
- `GET /api/orders/track/{orderNumber}` - Track order (public)

#### Payments
- `POST /api/payments/initialize` - Initialize payment
- `POST /api/payments/verify` - Verify payment
- `POST /api/payments/webhook` - Payment webhook (Chapa)

#### Admin
- `GET /api/admin/**` - Admin endpoints (requires ADMIN role)

## 📁 Project Structure

```
delivery/
├── src/
│   ├── main/
│   │   ├── java/com/kuru/delivery/
│   │   │   ├── auth/              # Authentication & authorization
│   │   │   ├── order/             # Order management
│   │   │   ├── payment/           # Payment processing
│   │   │   ├── driver/            # Driver management
│   │   │   ├── user/              # User management
│   │   │   ├── admin/             # Admin functionality
│   │   │   ├── pricing/           # Pricing configuration
│   │   │   ├── location/          # Location services
│   │   │   ├── message/           # Messaging system
│   │   │   ├── faq/               # FAQ management
│   │   │   ├── contact/           # Contact form
│   │   │   ├── config/            # Configuration classes
│   │   │   └── common/            # Shared utilities
│   │   └── resources/
│   │       └── application.properties
│   └── test/                      # Test files
├── pom.xml                        # Maven dependencies
├── .env                           # Environment variables (not committed)
└── README.md                      # This file
```

## 🔒 Security Features

### Authentication & Authorization
- JWT-based stateless authentication
- Role-based access control (RBAC)
- Token blacklisting for logout
- Password encryption using BCrypt
- Email verification for new accounts

### API Security
- CORS configuration with whitelisted origins
- Rate limiting to prevent abuse
- Input validation and sanitization
- SQL injection prevention (JPA/Hibernate)
- XSS protection
- CSRF protection (disabled for JWT, can be enabled for cookies)

### Payment Security
- Webhook signature verification (HMAC-SHA256)
- Amount validation to prevent fraud
- Transaction reference validation
- Secure payment gateway integration

### Data Protection
- Environment variable-based secret management
- Secure file upload validation (magic bytes)
- Path traversal prevention
- Secure error handling (no sensitive data exposure)

## 🗄️ Database Schema

The application uses JPA/Hibernate for database management. Key entities include:

- **User**: User accounts and authentication
- **Order**: Delivery orders
- **PaymentTransaction**: Payment records
- **Driver**: Driver profiles and information
- **DeliveryProof**: Delivery confirmation images
- **Message**: Customer-driver messages
- **ServiceOffering**: Delivery service types
- **PricingPlan**: Pricing configurations

## 🧪 Testing

### Run Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify
```

## 🚢 Deployment

### Production Checklist

1. **Environment Variables**: Ensure all production environment variables are set
2. **Database**: Configure PostgreSQL connection
3. **Redis**: Set up Redis instance
4. **SSL/TLS**: Configure HTTPS
5. **CORS**: Update allowed origins for production domain
6. **Logging**: Configure production logging levels
7. **Monitoring**: Set up application monitoring
8. **Backup**: Configure database backups

### Recommended Deployment Platforms

- **Render**: Easy deployment with PostgreSQL and Redis
- **AWS**: EC2, RDS, ElastiCache
- **Heroku**: Platform-as-a-Service
- **DigitalOcean**: Droplets with managed databases

### Build for Production

```bash
mvn clean package -DskipTests
```

## 🐛 Troubleshooting

### Common Issues

**Issue**: Application fails to start
- **Solution**: Check environment variables in `.env` file
- **Solution**: Ensure Redis is running
- **Solution**: Verify database connection

**Issue**: Payment initialization fails
- **Solution**: Verify Chapa API keys are correct
- **Solution**: Check payment gateway mode (SANDBOX/PRODUCTION)

**Issue**: Email sending fails
- **Solution**: Verify SMTP credentials
- **Solution**: Check if "Less secure app access" is enabled (Gmail)
- **Solution**: Use App Password for Gmail

**Issue**: WebSocket connection fails
- **Solution**: Check CORS configuration
- **Solution**: Verify WebSocket endpoint URLs

## 📞 Support

For technical support or questions:
- Review API documentation at `/swagger-ui.html`
- Check application logs for detailed error messages
- Contact the development team

## 📄 License

This project is proprietary software. All rights reserved.

## 🔄 Version History

- **v0.0.1-SNAPSHOT**: Initial release
  - Core order management
  - Payment integration
  - Real-time tracking
  - Admin dashboard

---

**Built with ❤️ using Spring Boot**
