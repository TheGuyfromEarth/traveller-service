package com.travolish.traveller.kyc.repository;

import com.travolish.traveller.kyc.entity.HostBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HostBankAccountRepository extends JpaRepository<HostBankAccount, Long> {
    
    @Query("SELECT hba FROM HostBankAccount hba WHERE hba.hostKYC.id = :hostKYCId")
    List<HostBankAccount> findByHostKYCId(@Param("hostKYCId") Long hostKYCId);
    
    @Query("SELECT hba FROM HostBankAccount hba WHERE hba.hostKYC.id = :hostKYCId " +
           "AND hba.isPrimary = true")
    Optional<HostBankAccount> findPrimaryBankAccount(@Param("hostKYCId") Long hostKYCId);
    
    @Query("SELECT hba FROM HostBankAccount hba WHERE hba.hostKYC.id = :hostKYCId " +
           "AND hba.verificationStatus = :status")
    List<HostBankAccount> findByHostKYCIdAndVerificationStatus(
        @Param("hostKYCId") Long hostKYCId,
        @Param("status") String status
    );
    
    @Query("SELECT hba FROM HostBankAccount hba WHERE hba.hostKYC.id = :hostKYCId " +
           "AND hba.accountNumber = :accountNumber")
    Optional<HostBankAccount> findByHostKYCIdAndAccountNumber(
        @Param("hostKYCId") Long hostKYCId,
        @Param("accountNumber") String accountNumber
    );
    
    @Query("SELECT COUNT(hba) FROM HostBankAccount hba WHERE hba.hostKYC.id = :hostKYCId " +
           "AND hba.verificationStatus = 'VERIFIED'")
    Integer countVerifiedBankAccounts(@Param("hostKYCId") Long hostKYCId);
    
    @Query("SELECT hba FROM HostBankAccount hba WHERE hba.verificationStatus = 'PENDING' " +
           "ORDER BY hba.createdAt ASC")
    List<HostBankAccount> findPendingBankAccounts();
    
    @Query("SELECT hba FROM HostBankAccount hba WHERE hba.hostKYC.id = :hostKYCId " +
           "AND hba.isActive = true")
    List<HostBankAccount> findActiveBankAccounts(@Param("hostKYCId") Long hostKYCId);
}
