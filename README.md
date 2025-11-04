# 🐔 Broiler Farm Management System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![DDD](https://img.shields.io/badge/Architecture-DDD-purple.svg)](https://martinfowler.com/tags/domain%20driven%20design.html)

> A comprehensive Domain-Driven Design (DDD) based enterprise system for managing broiler chicken farms from chick reception to slaughter delivery.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [Domain Model](#-domain-model)
- [Getting Started](#-getting-started)
- [Use Cases](#-use-cases)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Business Rules](#-business-rules)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

The **Broiler Farm Management System** is an enterprise-grade application designed to digitalize and optimize the entire lifecycle of broiler chicken production. From receiving day-old chicks from hatcheries to delivering mature birds to slaughterhouses, this system provides comprehensive tracking, monitoring, and management capabilities.

### Key Benefits

- 📊 **Real-time Monitoring**: Track lot performance, mortality rates, and consumption patterns daily
- 🔬 **Health Management**: Complete treatment tracking with automatic withdrawal period enforcement
- 📈 **Performance Analytics**: FCR, ADG, and mortality rate calculations with alerts
- 🚚 **Supply Chain Integration**: Seamless integration with Hatchery and Slaughterhouse subsystems
- 📦 **Inventory Management**: FIFO-based consumable stock management with expiration tracking
- ⚖️ **Compliance**: Automated withdrawal period enforcement to ensure food safety regulations

---

## ✨ Features

### Core Functionality

#### 🐣 **Chick Reception & Lot Management**
- Receive chicks from hatchery with quality assessment (alive, DOA, weak)
- Automatic lot creation and assignment to poultry houses
- Unique lot numbering system: `FARM-HOUSE-YYYY-MM-DD`
- House capacity validation and occupancy tracking

#### 📊 **Daily Operations**
- **Mortality Tracking**: Daily mortality sheets with cause analysis and disposal methods
- **Consumption Management**: Feed and water consumption tracking with per-bird calculations
- **Treatment Management**: Veterinary treatments with automatic medication consumption and withdrawal period tracking

#### 📈 **Weekly Performance Monitoring**
- Automated data aggregation from daily activities
- Weight sampling and performance metrics calculation (FCR, ADG)
- Trend analysis and alert generation for abnormal patterns
- Delivery eligibility assessment

#### 🚚 **Delivery Management**
- Automated delivery notice creation from observation data
- Withdrawal period validation (hard block)
- Integration with slaughterhouse systems for scheduling
- Partial delivery support

#### 📦 **Inventory & Stock Management**
- FIFO-based stock consumption by expiration date
- Automatic reorder point alerts
- Quality inspection for incoming consumables
- Batch tracking for traceability

#### 👥 **User Management & Security**
- Role-based access control (RBAC)
- Employee profiles with optional system access
- Password policies with expiration
- Account locking after failed login attempts

---

## 🏗️ Architecture

### Domain-Driven Design (DDD)

This application follows **Domain-Driven Design** principles with clear bounded contexts and aggregate boundaries.