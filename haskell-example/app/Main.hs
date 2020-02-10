{-# LANGUAGE TemplateHaskell #-}
{-# LANGUAGE DeriveFunctor #-}
{-# LANGUAGE FlexibleContexts #-}
module Main where

import Lib
import Control.Monad.State
import Control.Monad.Free as Free
import Control.Monad.Free.TH
import Data.Map (Map)
import qualified Data.Map as M

data KVStore next = Get' String (Maybe Int -> next)
                  | Put' String Int next
                  | Delete' String next
                    deriving (Functor)

type KVStoreM = Free KVStore

makeFree ''KVStore

program :: KVStoreM (Maybe Int)
program = do
  put' "wild-cats" 2
  put' "tame-cats" 5
  n <- get' "wild-cats"
  delete' "tame-cats"
  return n

compiler :: MonadState (Map String Int) m => KVStoreM a -> m a
compiler = Free.iterM run
  where
    run (Get' k f) = f =<< gets (M.lookup k)
    run (Delete' k n) = do
      modify $ M.delete k
      n
    run (Put' k v n) = do
      modify $ M.insert k v
      n

main :: IO ()
main = putStrLn $ show (runState (compiler program) M.empty)
