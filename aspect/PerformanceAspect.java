package com.example.signupauth.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final long SLOW_EXECUTION_THRESHOLD = 1000; // 1 second

    @Around("@annotation(trackPerformance)")
    public Object trackMethodPerformance(ProceedingJoinPoint joinPoint, TrackPerformance trackPerformance) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringTypeName();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > SLOW_EXECUTION_THRESHOLD) {
                logger.warn("Slow execution detected in {}.{}(). Execution time: {} ms", 
                          className, methodName, executionTime);
            }

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Exception in {}.{}(). Execution time: {} ms. Error: {}", 
                        className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }
} 