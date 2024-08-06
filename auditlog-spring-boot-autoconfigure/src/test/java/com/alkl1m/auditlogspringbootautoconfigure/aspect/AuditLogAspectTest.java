package com.alkl1m.auditlogspringbootautoconfigure.aspect;

import com.alkl1m.auditlogspringbootautoconfigure.annotation.AuditLog;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.LogLevel;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private AuditLog auditLog;

    @Mock
    private Logger logger;

    @Mock
    private Signature signature;

    @InjectMocks
    private AuditLogAspect auditLogAspect;

    @Test
    void testLogMethodData_withValidPayload_loggingData() throws Throwable {
        when(signature.getName()).thenReturn("testMethod");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", "arg2"});
        when(joinPoint.proceed()).thenReturn("result");

        when(auditLog.logLevel()).thenReturn(LogLevel.INFO);

        auditLogAspect.logMethodData(joinPoint, auditLog);

        verify(joinPoint, times(1)).proceed();
        verify(logger, times(1)).info(anyString());
    }

    @Test
    void testLogMethodData_withException_loggingException() throws Throwable {
        when(signature.getName()).thenReturn("testMethod");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", "arg2"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test exception"));

        assertThrows(RuntimeException.class, () -> auditLogAspect.logMethodData(joinPoint, auditLog));

        verify(joinPoint, times(1)).proceed();
        verify(logger, times(1)).error(anyString());
    }

    @Test
    void testLogMethodData_withNullReturn_givesNullResult() throws Throwable {
        when(signature.getName()).thenReturn("testMethod");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", "arg2"});
        when(joinPoint.proceed()).thenReturn(null);

        when(auditLog.logLevel()).thenReturn(LogLevel.INFO);

        auditLogAspect.logMethodData(joinPoint, auditLog);

        verify(joinPoint, times(1)).proceed();
        verify(logger, times(1)).info(anyString());
    }

}