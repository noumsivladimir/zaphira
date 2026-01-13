package com.zaphira.wallet.service;

//@Aspect
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class WalletPermissionAspect {
//
//    @Autowired
//    private WalletPermissionService permissionService;
//
//    @Before("@annotation(requiresPermission)")
//    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
//        Object[] args = joinPoint.getArgs();
//
//        String walletNumber = extractWalletNumber(args);
//        Long userId = extractUserId(args);
//        String action = requiresPermission.action();
//
//        if (!permissionService.hasPermission(userId, walletNumber, action)) {
//            throw new InsufficientPermissionException(
//                    "User " + userId + " does not have permission to " + action + " on wallet " + walletNumber
//            );
//        }
//    }
//
//    // ... méthodes d'extraction
//}